package com.example.proyectoIntegrador.services.Impl;

import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.enums.AppointmentStatus;
import com.example.proyectoIntegrador.models.*;
import com.example.proyectoIntegrador.repository.AgendaCitaRepository;
import com.example.proyectoIntegrador.repository.ProyectoRepository;
import com.example.proyectoIntegrador.repository.VeterinarianRepository;
import com.example.proyectoIntegrador.security.AuthenticatedUser;
import com.example.proyectoIntegrador.security.Role;
import org.springframework.security.access.AccessDeniedException;
import com.example.proyectoIntegrador.services.AgendaService;
import com.example.proyectoIntegrador.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgendaServiceImpl implements AgendaService {

    private final AgendaCitaRepository agendaCitaRepository;
    private final ProyectoRepository proyectoRepository;
    private final NotificationService notificationService;
    private final VeterinarianRepository veterinarianRepository;

    // STRICT con 'uuuu': rechaza fechas inexistentes como 20261332 o 20260230
    private static final DateTimeFormatter STRICT_DATE =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter STRICT_TIME =
            DateTimeFormatter.ofPattern("HHmmss").withResolverStyle(ResolverStyle.STRICT);

    @Override
    public List<AgendaCita> getListCitas(int idClient) {
        log.debug("start getListCitas");

        agendaCitaRepository.validateClient(idClient)
                .orElseThrow(() -> new BusinessException("cliente not found"));

        return agendaCitaRepository.getListAgenda(idClient)
                .orElseThrow(() -> new BusinessException("El usuario no tiene citas agendadas"));
    }

    @Override
    public boolean scheduleAnAppointment(DataAppointmentDTO dataAppointmentDTO) {
        log.debug("start scheduleAnAppointment");

        LocalDate date = parseDate(dataAppointmentDTO.getFecha());
        LocalTime time = parseTime(dataAppointmentDTO.getHora());
        requireFuture(date, time);

        agendaCitaRepository.validateClient(dataAppointmentDTO.getClienteId())
                .orElseThrow(() -> new BusinessException("cliente not found"));

        if (!proyectoRepository.isUserActive(dataAppointmentDTO.getClienteId())) {
            throw new BusinessException("El cliente esta inactivo");
        }

        agendaCitaRepository.validateVeterinarian(dataAppointmentDTO.getMedicoId())
                .orElseThrow(() -> new BusinessException("El veterinario no existe o esta inactivo"));

        // La mascota debe ser del cliente de la cita
        if (!agendaCitaRepository.isMascotaOfClient(dataAppointmentDTO.getMascotaId(), dataAppointmentDTO.getClienteId())) {
            throw new BusinessException("La mascota no existe, no pertenece al cliente o esta inactiva");
        }

        if (!agendaCitaRepository.isSlotAvailable(dataAppointmentDTO.getMedicoId(), date, time, null)) {
            throw new BusinessException("El veterinario no tiene disponible ese horario");
        }

        // Toda cita nueva empieza como programada: el estado no lo decide quien agenda
        int programada = estadoId(AppointmentStatus.PROGRAMADA);

        agendaCitaRepository.insertAgendaCita(dataAppointmentDTO.getDescription(), dataAppointmentDTO.getFecha(),
                        time, programada, dataAppointmentDTO.getClienteId(), dataAppointmentDTO.getMedicoId(), dataAppointmentDTO.getMascotaId())
                .orElseThrow(() -> new BusinessException("No se pudo agendar la cita"));

        notificationService.notifyAppointmentCreation(String.valueOf(dataAppointmentDTO.getClienteId()), new DetailAppointment(dataAppointmentDTO.getDescription(),
                dataAppointmentDTO.getFecha(),
                dataAppointmentDTO.getHora()));


        return true;
    }

    @Override
    public AgendaCitaDetail getDetailAppointment(DataDetailAppointmentDTO detailAppointmentDTO) {

        var agendaCitaDetail = agendaCitaRepository.getDetailAppointment(detailAppointmentDTO.getIdAppointment(), detailAppointmentDTO.getIdClient())
                .orElseThrow(() -> new BusinessException("No se encontro la cita programada"));

        var infoUser = proyectoRepository.validateUser(detailAppointmentDTO.getUsername())
                .orElseThrow(() -> {
                    log.debug("Error. El usuario no esta registrado");
                    return new BusinessException("Error. El usuario no esta registrado");
                });

        var infoMascota = agendaCitaRepository.getInfoMascota(detailAppointmentDTO.getIdClient(), agendaCitaDetail.getMascotaId())
                .orElseThrow(() -> new BusinessException("El usuario no tiene una mascota registrada"));

        agendaCitaDetail.setInfoUser(infoUser);
        agendaCitaDetail.setInfoMascota(infoMascota);

        return agendaCitaDetail;
    }

    /**
     * Cancela una cita pendiente del cliente. Los estados se reconocen por su descripción
     * (antes se usaban los ids 3 y 1, 2, 4 fijos en el SQL).
     */
    @Override
    public boolean cancelledAppointment(DataCancelledAppointmentDTO dataCancelledAppointmentDTO) {

        agendaCitaRepository.validateClient(dataCancelledAppointmentDTO.getIdCliente())
                .orElseThrow(() -> new BusinessException("cliente not found"));

        VeterinarianAgendaItem cita = agendaCitaRepository.findAgendaItem(dataCancelledAppointmentDTO.getIdAppointment())
                .filter(c -> dataCancelledAppointmentDTO.getIdCliente().equals(c.getClienteId()))
                .orElseThrow(() -> new BusinessException("La cita no existe"));

        if (!AppointmentStatus.fromDescription(cita.getEstado()).isPending()) {
            throw new BusinessException("No se puede cancelar una cita en estado '" + cita.getEstado() + "'");
        }

        int cancelada = estadoId(AppointmentStatus.CANCELADA);
        if (agendaCitaRepository.updateEstado(cita.getAgendaId(), cita.getEstadoId(), cancelada) == 0) {
            throw new BusinessException("La cita cambio de estado mientras se actualizaba. Consultela de nuevo");
        }

        return true;
    }

    /**
     * Reprograma una cita pendiente a otra fecha y hora (y opcionalmente otro veterinario) (VET-04).
     * El cliente solo sus citas, el veterinario las asignadas a él y el administrador cualquiera.
     */
    @Override
    @Transactional
    public VeterinarianAgendaItem rescheduleAppointment(AuthenticatedUser user, int agendaId, RescheduleAppointmentDTO dto) {
        log.debug("start rescheduleAppointment agenda={}", agendaId);

        VeterinarianAgendaItem cita = agendaCitaRepository.findAgendaItem(agendaId)
                .orElseThrow(() -> new BusinessException("La cita no existe"));

        requireCanReschedule(user, cita);

        if (!AppointmentStatus.fromDescription(cita.getEstado()).isPending()) {
            throw new BusinessException("No se puede reprogramar una cita en estado '" + cita.getEstado() + "'");
        }

        LocalDate fecha = parseDate(dto.getFecha());
        LocalTime hora = parseTime(dto.getHora());
        requireFuture(fecha, hora);

        int medicoId = dto.getMedicoId() != null ? dto.getMedicoId() : cita.getMedicoId();
        agendaCitaRepository.validateVeterinarian(medicoId)
                .orElseThrow(() -> new BusinessException("El veterinario no existe o esta inactivo"));

        // La propia cita no cuenta: así se puede mover dentro de su mismo horario
        if (!agendaCitaRepository.isSlotAvailable(medicoId, fecha, hora, agendaId)) {
            throw new BusinessException("El veterinario no tiene disponible ese horario");
        }

        int reprogramada = estadoId(AppointmentStatus.REPROGRAMADA);
        if (agendaCitaRepository.reschedule(agendaId, cita.getEstadoId(), fecha, hora, medicoId, reprogramada) == 0) {
            throw new BusinessException("La cita cambio de estado mientras se actualizaba. Consultela de nuevo");
        }

        log.info("Cita {} reprogramada a {} {} con el medico {} por el usuario {}", agendaId, fecha, hora, medicoId, user.userId());
        return agendaCitaRepository.findAgendaItem(agendaId)
                .orElseThrow(() -> new RuntimeException("No se encontro la cita recien reprogramada"));
    }

    /**
     * El administrador reprograma cualquier cita; el veterinario, las asignadas a él; el cliente, las suyas.
     */
    private void requireCanReschedule(AuthenticatedUser user, VeterinarianAgendaItem cita) {
        if (user.role() == Role.CLIENTE) {
            if (cita.getClienteId() != null && cita.getClienteId() == user.userId()) {
                return;
            }
            throw new AccessDeniedException("Solo puede reprogramar sus propias citas");
        }
        requireCanManage(user, cita);
    }

    /**
     * id del estado en estado_cita. Si no existe es un problema de configuración de la base de
     * datos (ver sql/VET-04_reprogramacion_citas.sql), no un error del usuario.
     */
    private int estadoId(AppointmentStatus status) {
        return agendaCitaRepository.findEstadoId(status)
                .orElseThrow(() -> new RuntimeException("No existe el estado " + status + " en la tabla estado_cita"));
    }

    @Override
    public List<VeterinarianAgendaItem> getVeterinarianAgenda(AuthenticatedUser user, DataVeterinarianAgendaDTO dto) {
        log.debug("start getVeterinarianAgenda");

        int medicoId = resolveMedicoId(user, dto.getMedicoId());
        LocalDate fecha = parseDate(dto.getFecha());

        return agendaCitaRepository.getAgendaByVeterinarian(medicoId, fecha, dto.getEstadoId());
    }

    @Override
    public VeterinarianAgendaItem changeAppointmentStatus(AuthenticatedUser user, int agendaId, int estadoId) {
        log.debug("start changeAppointmentStatus agenda={} estado={}", agendaId, estadoId);

        VeterinarianAgendaItem cita = agendaCitaRepository.findAgendaItem(agendaId)
                .orElseThrow(() -> new BusinessException("La cita no existe"));

        requireCanManage(user, cita);

        String targetDescription = agendaCitaRepository.getEstadoDescription(estadoId)
                .orElseThrow(() -> new BusinessException("El estado no existe"));

        AppointmentStatus current = AppointmentStatus.fromDescription(cita.getEstado());
        AppointmentStatus target = AppointmentStatus.fromDescription(targetDescription);

        if (!current.canTransitionTo(target)) {
            throw new BusinessException("No se puede cambiar una cita en estado '" + cita.getEstado()
                    + "' a '" + targetDescription + "'");
        }

        // Solo actualiza si la cita sigue en el estado que se validó
        if (agendaCitaRepository.updateEstado(agendaId, cita.getEstadoId(), estadoId) == 0) {
            throw new BusinessException("La cita cambio de estado mientras se actualizaba. Consultela de nuevo");
        }

        log.info("Cita {} cambiada de {} a {} por el usuario {}", agendaId, current, target, user.userId());
        return agendaCitaRepository.findAgendaItem(agendaId)
                .orElseThrow(() -> new RuntimeException("No se encontro la cita recien actualizada"));
    }

    /**
     * El administrador gestiona cualquier cita; el veterinario, solo las asignadas a él.
     */
    private void requireCanManage(AuthenticatedUser user, VeterinarianAgendaItem cita) {
        if (user.role() == Role.ADMIN) {
            return;
        }
        if (user.role() == Role.VETERINARIO) {
            int own = veterinarianRepository.findMedicoIdByClienteId(user.userId())
                    .orElseThrow(() -> new BusinessException("Su usuario no esta vinculado a un veterinario"));
            if (cita.getMedicoId() != null && cita.getMedicoId() == own) {
                return;
            }
            throw new AccessDeniedException("Un veterinario solo puede gestionar sus propias citas");
        }
        throw new AccessDeniedException("Solo el personal puede cambiar el estado de las citas");
    }

    private int resolveMedicoId(AuthenticatedUser user, Integer requestedMedicoId) {
        if (user.role() == Role.VETERINARIO) {
            int own = veterinarianRepository.findMedicoIdByClienteId(user.userId())
                    .orElseThrow(() -> new BusinessException("Su usuario no esta vinculado a un veterinario"));
            if (requestedMedicoId != null && requestedMedicoId != own) {
                throw new AccessDeniedException("Un veterinario solo puede consultar su propia agenda");
            }
            return own;
        }

        if (user.role() == Role.ADMIN) {
            if (requestedMedicoId == null) {
                throw new BusinessException("Debe indicar el veterinario (medicoId)");
            }
            veterinarianRepository.findById(requestedMedicoId)
                    .orElseThrow(() -> new BusinessException("El veterinario no existe"));
            return requestedMedicoId;
        }

        // El controlador ya lo restringe con @PreAuthorize; esto es una segunda barrera
        throw new AccessDeniedException("Solo el personal puede consultar la agenda de un veterinario");
    }

    private static LocalDate parseDate(String fecha) {
        try {
            return LocalDate.parse(fecha, STRICT_DATE);
        } catch (DateTimeParseException e) {
            throw new BusinessException("La fecha no es valida");
        }
    }

    private static LocalTime parseTime(String hora) {
        try {
            return LocalTime.parse(hora, STRICT_TIME);
        } catch (DateTimeParseException e) {
            throw new BusinessException("La hora no es valida");
        }
    }

    private static void requireFuture(LocalDate fecha, LocalTime hora) {
        if (!LocalDateTime.of(fecha, hora).isAfter(LocalDateTime.now())) {
            throw new BusinessException("La fecha y hora de la cita ya pasaron");
        }
    }

}
