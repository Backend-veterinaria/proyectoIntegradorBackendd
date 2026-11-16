package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.models.ClinicalAttention;
import com.example.proyectoIntegrador.models.ClinicalAttentionCreateDTO;
import com.example.proyectoIntegrador.security.AuthenticatedUser;

public interface ClinicalHistoryService {

    /**
     * Registra la atención clínica de una cita y la marca como atendida (HC-01).
     */
    ClinicalAttention registerAttention(AuthenticatedUser user, ClinicalAttentionCreateDTO dto);

    ClinicalAttention getAttention(AuthenticatedUser user, int atencionId);
}
