package com.example.proyectoIntegrador.services.Impl;

import com.example.proyectoIntegrador.enums.AppointmentStatus;
import com.example.proyectoIntegrador.enums.TreatmentStatus;
import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.ClinicalAttention;
import com.example.proyectoIntegrador.models.ClinicalAttentionCreateDTO;
import com.example.proyectoIntegrador.models.ClinicalHistory;
import com.example.proyectoIntegrador.models.InfoMascota;
import com.example.proyectoIntegrador.models.Treatment;
import com.example.proyectoIntegrador.models.TreatmentCreateDTO;
import com.example.proyectoIntegrador.models.Vaccine;
import com.example.proyectoIntegrador.models.VaccineCreateDTO;
import com.example.proyectoIntegrador.models.VeterinarianAgendaItem;
import com.example.proyectoIntegrador.repository.AgendaCitaRepository;
import com.example.proyectoIntegrador.repository.ClinicalHistoryRepository;
import com.example.proyectoIntegrador.repository.PetsRepository;
import com.example.proyectoIntegrador.repository.TreatmentVaccineRepository;
import com.example.proyectoIntegrador.repository.VeterinarianRepository;
import com.example.proyectoIntegrador.security.AuthenticatedUser;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.security.SecurityUtils;
import com.example.proyectoIntegrador.services.ClinicalHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

/**
 * Historia clínica (RF-09). La historia clínica es privada y está sometida a reserva
 * (Ley 576 de 2000, art. 61): solo el veterinario de la cita registra la atención, y lo
 * registrado no se edita ni se borra desde la API.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicalHistoryServiceImpl implements ClinicalHistoryService {

    private final ClinicalHistoryRepository clinicalHistoryRepository;
    private final AgendaCitaRepository agendaCitaRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final PetsRepository petsRepository;
    private final TreatmentVaccineRepository treatmentVaccineRepository;

    /**
     * Registra la atención de una cita (HC-01):
     * <ul>
     *   <li>Solo el veterinario asignado a la cita.</li>
     *   <li>La cita debe estar pendiente (pasa a atendida) o ya marcada como atendida sin atención
     *       registrada (por ejemplo, si se cambió el estado con VET-03 antes de registrarla).</li>
     *   <li>Una sola atención por cita, y no antes del día de la cita.</li>
     * </ul>
     */
    @Override
    @Transactional
    public ClinicalAttention registerAttention(AuthenticatedUser user, ClinicalAttentionCreateDTO dto) {
        log.debug("start registerAttention agenda={}", dto.getAgendaId());

        int medicoId = requireVeterinarian(user, "Solo un veterinario puede registrar la atencion clinica");

        VeterinarianAgendaItem cita = agendaCitaRepository.findAgendaItem(dto.getAgendaId())
                .orElseThrow(() -> new BusinessException("La cita no existe"));

        if (cita.getMedicoId() == null || cita.getMedicoId() != medicoId) {
            throw new AccessDeniedException("Solo el veterinario asignado puede registrar la atencion de la cita");
        }
        if (cita.getMascotaId() == null) {
            throw new BusinessException("La cita no tiene una mascota asociada");
        }
        if (citaDate(cita).isAfter(LocalDate.now())) {
            throw new BusinessException("No se puede registrar la atencion antes del dia de la cita");
        }
        if (clinicalHistoryRepository.existsByAgendaId(cita.getAgendaId())) {
            throw new BusinessException("La cita ya tiene una atencion registrada");
        }

        AppointmentStatus current = AppointmentStatus.fromDescription(cita.getEstado());
        if (current.canTransitionTo(AppointmentStatus.ATENDIDA)) {
            int atendida = agendaCitaRepository.findEstadoId(AppointmentStatus.ATENDIDA)
                    .orElseThrow(() -> new RuntimeException("No existe el estado ATENDIDA en la tabla estado_cita"));
            // Solo si la cita sigue en el estado que se validó
            if (agendaCitaRepository.updateEstado(cita.getAgendaId(), cita.getEstadoId(), atendida) == 0) {
                throw new BusinessException("La cita cambio de estado mientras se actualizaba. Consultela de nuevo");
            }
        } else if (current != AppointmentStatus.ATENDIDA) {
            throw new BusinessException("No se puede registrar la atencion de una cita en estado '" + cita.getEstado() + "'");
        }

        int atencionId;
        try {
            atencionId = clinicalHistoryRepository.insert(normalize(dto), cita.getMascotaId(), medicoId);
        } catch (DuplicateKeyException e) {
            // Dos registros a la vez para la misma cita: lo impide la restricción UNIQUE de agenda_id
            throw new BusinessException("La cita ya tiene una atencion registrada");
        }

        log.info("Atencion {} registrada para la cita {} por el medico {}", atencionId, cita.getAgendaId(), medicoId);
        return clinicalHistoryRepository.findById(atencionId)
                .orElseThrow(() -> new RuntimeException("No se encontro la atencion recien registrada"));
    }

    /**
     * Consulta una atención: el personal cualquiera, el dueño solo las de sus mascotas.
     * La reserva de la historia clínica es frente a terceros, no frente al propietario del animal.
     */
    @Override
    public ClinicalAttention getAttention(AuthenticatedUser user, int atencionId) {
        ClinicalAttention atencion = clinicalHistoryRepository.findById(atencionId)
                .orElseThrow(() -> new BusinessException("La atencion no existe"));
        if (!SecurityUtils.canAccessClient(user, atencion.getClienteId())) {
            throw new AccessDeniedException("Solo puede consultar la historia clinica de sus mascotas");
        }
        return atencion;
    }

    /**
     * Historia clínica de una mascota (HC-03), incluidas las mascotas inactivas: la historia se conserva.
     */
    @Override
    public ClinicalHistory getPetHistory(AuthenticatedUser user, int mascotaId) {
        log.debug("start getPetHistory mascota={}", mascotaId);

        InfoMascota mascota = petsRepository.findById(mascotaId)
                .orElseThrow(() -> new BusinessException("La mascota no existe"));
        if (!SecurityUtils.canAccessClient(user, mascota.getClienteId())) {
            throw new AccessDeniedException("Solo puede consultar la historia clinica de sus mascotas");
        }

        return new ClinicalHistory(mascota,
                clinicalHistoryRepository.findByMascotaId(mascotaId),
                treatmentVaccineRepository.findTreatmentsByMascota(mascotaId),
                treatmentVaccineRepository.findVaccinesByMascota(mascotaId));
    }

    /**
     * Registra un tratamiento (HC-04). Cualquier veterinario, a una mascota activa; si se indica la
     * atención en la que se formuló, debe ser de la misma mascota.
     */
    @Override
    public Treatment registerTreatment(AuthenticatedUser user, TreatmentCreateDTO dto) {
        log.debug("start registerTreatment mascota={}", dto.getMascotaId());

        int medicoId = requireVeterinarian(user, "Solo un veterinario puede registrar tratamientos");
        requireActivePet(dto.getMascotaId());
        requireAttentionOfPet(dto.getAtencionId(), dto.getMascotaId());

        LocalDate inicio = parseDate(dto.getFechaInicio(), "La fecha de inicio no es valida");
        LocalDate fin = dto.getFechaFin() == null ? null : parseDate(dto.getFechaFin(), "La fecha de fin no es valida");
        if (fin != null && fin.isBefore(inicio)) {
            throw new BusinessException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }

        int id = treatmentVaccineRepository.insertTreatment(dto.getMascotaId(), dto.getAtencionId(), medicoId,
                trimToNull(dto.getMedicamento()), trimToNull(dto.getDosis()), trimToNull(dto.getFrecuencia()),
                trimToNull(dto.getIndicaciones()), inicio, fin);

        log.info("Tratamiento {} registrado para la mascota {} por el medico {}", id, dto.getMascotaId(), medicoId);
        return treatmentVaccineRepository.findTreatment(id)
                .orElseThrow(() -> new RuntimeException("No se encontro el tratamiento recien registrado"));
    }

    /**
     * Seguimiento de un tratamiento (HC-04): un veterinario lo finaliza o lo suspende; la fecha de fin
     * queda en la fecha de hoy.
     */
    @Override
    public Treatment changeTreatmentStatus(AuthenticatedUser user, int tratamientoId, TreatmentStatus estado) {
        log.debug("start changeTreatmentStatus {} -> {}", tratamientoId, estado);

        requireVeterinarian(user, "Solo un veterinario puede hacer seguimiento a los tratamientos");

        Treatment tratamiento = treatmentVaccineRepository.findTreatment(tratamientoId)
                .orElseThrow(() -> new BusinessException("El tratamiento no existe"));

        TreatmentStatus current = TreatmentStatus.valueOf(tratamiento.getEstado());
        if (!current.canTransitionTo(estado)) {
            throw new BusinessException("No se puede cambiar un tratamiento " + current + " a " + estado);
        }
        if (treatmentVaccineRepository.updateTreatmentStatus(tratamientoId, current, estado, LocalDate.now()) == 0) {
            throw new BusinessException("El tratamiento cambio de estado mientras se actualizaba. Consultelo de nuevo");
        }

        return treatmentVaccineRepository.findTreatment(tratamientoId)
                .orElseThrow(() -> new RuntimeException("No se encontro el tratamiento recien actualizado"));
    }

    /**
     * Registra una vacuna aplicada (HC-05). Cualquier veterinario, a una mascota activa. La fecha de
     * aplicación no puede ser futura y la próxima dosis debe ser posterior a la aplicación.
     */
    @Override
    public Vaccine registerVaccine(AuthenticatedUser user, VaccineCreateDTO dto) {
        log.debug("start registerVaccine mascota={}", dto.getMascotaId());

        int medicoId = requireVeterinarian(user, "Solo un veterinario puede registrar vacunas");
        requireActivePet(dto.getMascotaId());
        requireAttentionOfPet(dto.getAtencionId(), dto.getMascotaId());

        LocalDate aplicacion = parseDate(dto.getFechaAplicacion(), "La fecha de aplicacion no es valida");
        if (aplicacion.isAfter(LocalDate.now())) {
            throw new BusinessException("La fecha de aplicacion no puede ser futura");
        }
        LocalDate proxima = dto.getProximaDosis() == null ? null
                : parseDate(dto.getProximaDosis(), "La fecha de la proxima dosis no es valida");
        if (proxima != null && !proxima.isAfter(aplicacion)) {
            throw new BusinessException("La proxima dosis debe ser posterior a la fecha de aplicacion");
        }

        int id = treatmentVaccineRepository.insertVaccine(dto.getMascotaId(), dto.getAtencionId(), medicoId,
                trimToNull(dto.getNombre()), trimToNull(dto.getLote()), aplicacion, proxima,
                trimToNull(dto.getObservaciones()));

        log.info("Vacuna {} registrada para la mascota {} por el medico {}", id, dto.getMascotaId(), medicoId);
        return treatmentVaccineRepository.findVaccine(id)
                .orElseThrow(() -> new RuntimeException("No se encontro la vacuna recien registrada"));
    }

    /**
     * Vacunas con próxima dosis vencida o que vence en los próximos {@code days} días (HC-05).
     * El personal ve todas (o las de un cliente); el cliente, solo las de sus mascotas.
     */
    @Override
    public List<Vaccine> getUpcomingVaccines(AuthenticatedUser user, int days, Integer clienteId) {
        if (days < 0 || days > MAX_UPCOMING_DAYS) {
            throw new BusinessException("Los dias deben estar entre 0 y " + MAX_UPCOMING_DAYS);
        }
        Integer filter = clienteId;
        if (!user.role().isStaff()) {
            if (clienteId != null && clienteId != user.userId()) {
                throw new AccessDeniedException("Solo puede consultar las vacunas de sus mascotas");
            }
            filter = user.userId();
        }
        return treatmentVaccineRepository.findUpcomingVaccines(LocalDate.now().plusDays(days), filter);
    }

    /** Máximo de días hacia adelante en el seguimiento de vacunas. */
    static final int MAX_UPCOMING_DAYS = 365;

    // STRICT con 'uuuu': rechaza fechas inexistentes como 20260230
    private static final DateTimeFormatter STRICT_DATE =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    /**
     * @return el medicoId del veterinario autenticado
     */
    private int requireVeterinarian(AuthenticatedUser user, String deniedMessage) {
        if (user.role() != Role.VETERINARIO) {
            throw new AccessDeniedException(deniedMessage);
        }
        return veterinarianRepository.findMedicoIdByClienteId(user.userId())
                .orElseThrow(() -> new BusinessException("Su usuario no esta vinculado a un veterinario"));
    }

    private void requireActivePet(int mascotaId) {
        InfoMascota mascota = petsRepository.findById(mascotaId)
                .orElseThrow(() -> new BusinessException("La mascota no existe"));
        if (Boolean.FALSE.equals(mascota.getActivo())) {
            throw new BusinessException("La mascota esta inactiva");
        }
    }

    private void requireAttentionOfPet(Integer atencionId, int mascotaId) {
        if (atencionId == null) {
            return;
        }
        clinicalHistoryRepository.findById(atencionId)
                .filter(a -> a.getMascotaId() != null && a.getMascotaId() == mascotaId)
                .orElseThrow(() -> new BusinessException("La atencion no existe o no es de esta mascota"));
    }

    private static LocalDate parseDate(String value, String message) {
        try {
            return LocalDate.parse(value, STRICT_DATE);
        } catch (DateTimeParseException e) {
            throw new BusinessException(message);
        }
    }

    /** c.fecha es DATE: JDBC la devuelve como yyyy-MM-dd. */
    private static LocalDate citaDate(VeterinarianAgendaItem cita) {
        try {
            return LocalDate.parse(cita.getFecha().substring(0, 10));
        } catch (NullPointerException | IndexOutOfBoundsException | DateTimeParseException e) {
            throw new RuntimeException("Fecha de cita no reconocida: " + cita.getFecha(), e);
        }
    }

    private static ClinicalAttentionCreateDTO normalize(ClinicalAttentionCreateDTO dto) {
        ClinicalAttentionCreateDTO clean = new ClinicalAttentionCreateDTO();
        clean.setAgendaId(dto.getAgendaId());
        clean.setMotivoConsulta(trimToNull(dto.getMotivoConsulta()));
        clean.setDiagnostico(trimToNull(dto.getDiagnostico()));
        clean.setProcedimientos(trimToNull(dto.getProcedimientos()));
        clean.setObservaciones(trimToNull(dto.getObservaciones()));
        clean.setPesoKg(dto.getPesoKg());
        clean.setTemperaturaC(dto.getTemperaturaC());
        return clean;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
