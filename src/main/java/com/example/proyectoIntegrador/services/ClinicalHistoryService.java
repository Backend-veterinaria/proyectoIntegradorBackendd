package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.models.ClinicalAttention;
import com.example.proyectoIntegrador.models.ClinicalAttentionCreateDTO;
import com.example.proyectoIntegrador.models.ClinicalHistory;
import com.example.proyectoIntegrador.enums.TreatmentStatus;
import com.example.proyectoIntegrador.models.Treatment;
import com.example.proyectoIntegrador.models.TreatmentCreateDTO;
import com.example.proyectoIntegrador.models.Vaccine;
import com.example.proyectoIntegrador.models.VaccineCreateDTO;
import com.example.proyectoIntegrador.security.AuthenticatedUser;

import java.util.List;

public interface ClinicalHistoryService {

    /**
     * Registra la atención clínica de una cita y la marca como atendida (HC-01).
     */
    ClinicalAttention registerAttention(AuthenticatedUser user, ClinicalAttentionCreateDTO dto);

    ClinicalAttention getAttention(AuthenticatedUser user, int atencionId);

    /**
     * Historia clínica de una mascota (HC-03): el personal la de cualquiera, el dueño la de las suyas.
     */
    ClinicalHistory getPetHistory(AuthenticatedUser user, int mascotaId);

    /** Registra un tratamiento (HC-04). */
    Treatment registerTreatment(AuthenticatedUser user, TreatmentCreateDTO dto);

    /** Seguimiento de un tratamiento: finalizarlo o suspenderlo (HC-04). */
    Treatment changeTreatmentStatus(AuthenticatedUser user, int tratamientoId, TreatmentStatus estado);

    /** Registra una vacuna aplicada (HC-05). */
    Vaccine registerVaccine(AuthenticatedUser user, VaccineCreateDTO dto);

    /** Vacunas con la próxima dosis vencida o por vencer (HC-05). */
    List<Vaccine> getUpcomingVaccines(AuthenticatedUser user, int days, Integer clienteId);
}
