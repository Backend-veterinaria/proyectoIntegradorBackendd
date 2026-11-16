package com.example.proyectoIntegrador.services.Impl;

import com.example.proyectoIntegrador.enums.AppointmentStatus;
import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.ClinicalAttention;
import com.example.proyectoIntegrador.models.ClinicalAttentionCreateDTO;
import com.example.proyectoIntegrador.models.VeterinarianAgendaItem;
import com.example.proyectoIntegrador.repository.AgendaCitaRepository;
import com.example.proyectoIntegrador.repository.ClinicalHistoryRepository;
import com.example.proyectoIntegrador.repository.VeterinarianRepository;
import com.example.proyectoIntegrador.security.AuthenticatedUser;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.services.ClinicalHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

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

        if (user.role() != Role.VETERINARIO) {
            throw new AccessDeniedException("Solo un veterinario puede registrar la atencion clinica");
        }
        int medicoId = veterinarianRepository.findMedicoIdByClienteId(user.userId())
                .orElseThrow(() -> new BusinessException("Su usuario no esta vinculado a un veterinario"));

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
     * Consulta una atención. Por la reserva de la historia clínica, solo el personal.
     */
    @Override
    public ClinicalAttention getAttention(AuthenticatedUser user, int atencionId) {
        if (!user.role().isStaff()) {
            throw new AccessDeniedException("Solo el personal puede consultar la atencion clinica");
        }
        return clinicalHistoryRepository.findById(atencionId)
                .orElseThrow(() -> new BusinessException("La atencion no existe"));
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
