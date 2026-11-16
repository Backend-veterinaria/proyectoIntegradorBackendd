package com.example.proyectoIntegrador.services.Impl;

import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.Report;
import com.example.proyectoIntegrador.models.ReportFilterDTO;
import com.example.proyectoIntegrador.models.ReportRows;
import com.example.proyectoIntegrador.repository.ReportRepository;
import com.example.proyectoIntegrador.repository.VeterinarianRepository;
import com.example.proyectoIntegrador.security.AuthenticatedUser;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.services.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    /** Período máximo de un reporte, para no recorrer toda la base de datos de una vez. */
    static final int MAX_DAYS = 366;

    // STRICT con 'uuuu': rechaza fechas inexistentes como 20260230
    private static final DateTimeFormatter STRICT_DATE =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    private final ReportRepository reportRepository;
    private final VeterinarianRepository veterinarianRepository;

    @Override
    public Report<ReportRows.AppointmentsByStatus> appointmentsByStatus(AuthenticatedUser user, ReportFilterDTO filter) {
        Period period = resolve(user, filter);
        List<ReportRows.AppointmentsByStatus> rows =
                reportRepository.appointmentsByStatus(period.desde(), period.hasta(), period.medicoId());
        long total = rows.stream().mapToLong(ReportRows.AppointmentsByStatus::cantidad).sum();
        return period.report(total, rows);
    }

    @Override
    public Report<ReportRows.AttentionsByVeterinarian> attentionsByVeterinarian(AuthenticatedUser user,
                                                                                ReportFilterDTO filter) {
        Period period = resolve(user, filter);
        List<ReportRows.AttentionsByVeterinarian> rows =
                reportRepository.attentionsByVeterinarian(period.desde(), period.hasta(), period.medicoId());
        long total = rows.stream().mapToLong(ReportRows.AttentionsByVeterinarian::atenciones).sum();
        return period.report(total, rows);
    }

    @Override
    public Report<ReportRows.AttendedPet> attendedPets(AuthenticatedUser user, ReportFilterDTO filter) {
        Period period = resolve(user, filter);
        List<ReportRows.AttendedPet> rows =
                reportRepository.attendedPets(period.desde(), period.hasta(), period.medicoId());
        // Total = número de mascotas distintas atendidas
        return period.report(rows.size(), rows);
    }

    private record Period(LocalDate desde, LocalDate hasta, Integer medicoId) {
        <T> Report<T> report(long total, List<T> items) {
            return new Report<>(desde.toString(), hasta.toString(), medicoId, total, items);
        }
    }

    private Period resolve(AuthenticatedUser user, ReportFilterDTO filter) {
        LocalDate desde = parseDate(filter.getDesde(), "La fecha desde no es valida");
        LocalDate hasta = parseDate(filter.getHasta(), "La fecha hasta no es valida");
        if (hasta.isBefore(desde)) {
            throw new BusinessException("La fecha hasta no puede ser anterior a la fecha desde");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > MAX_DAYS) {
            throw new BusinessException("El periodo no puede ser mayor a " + MAX_DAYS + " dias");
        }
        return new Period(desde, hasta, resolveMedicoId(user, filter.getMedicoId()));
    }

    /**
     * El veterinario siempre ve solo sus datos; el administrador, todos o los de un veterinario.
     */
    private Integer resolveMedicoId(AuthenticatedUser user, Integer requested) {
        if (user.role() == Role.VETERINARIO) {
            int own = veterinarianRepository.findMedicoIdByClienteId(user.userId())
                    .orElseThrow(() -> new BusinessException("Su usuario no esta vinculado a un veterinario"));
            if (requested != null && requested != own) {
                throw new AccessDeniedException("Un veterinario solo puede consultar sus propios reportes");
            }
            return own;
        }
        if (user.role() == Role.ADMIN) {
            if (requested != null) {
                veterinarianRepository.findById(requested)
                        .orElseThrow(() -> new BusinessException("El veterinario no existe"));
            }
            return requested;
        }
        // El controlador ya lo restringe con @PreAuthorize; esto es una segunda barrera
        throw new AccessDeniedException("Solo el personal puede consultar los reportes");
    }

    private static LocalDate parseDate(String value, String message) {
        try {
            return LocalDate.parse(value, STRICT_DATE);
        } catch (DateTimeParseException e) {
            throw new BusinessException(message);
        }
    }
}
