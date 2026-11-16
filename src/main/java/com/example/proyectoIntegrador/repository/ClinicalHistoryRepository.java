package com.example.proyectoIntegrador.repository;

import com.example.proyectoIntegrador.models.ClinicalAttention;
import com.example.proyectoIntegrador.models.ClinicalAttentionCreateDTO;

import java.util.List;
import java.util.Optional;

/**
 * Historia clínica (HC-01). Requiere sql/HC-01_atencion_clinica.sql.
 */
public interface ClinicalHistoryRepository {

    /**
     * Indica si la cita ya tiene una atención registrada. No captura excepciones.
     */
    boolean existsByAgendaId(int agendaId);

    /**
     * Inserta la atención. Debe llamarse dentro de una transacción. No captura excepciones.
     *
     * @return el atencion_id generado
     */
    int insert(ClinicalAttentionCreateDTO dto, int mascotaId, int medicoId);

    /**
     * Atenciones de una mascota, de la más reciente a la más antigua (HC-03). No captura excepciones.
     */
    List<ClinicalAttention> findByMascotaId(int mascotaId);

    /**
     * @return la atención con el nombre de la mascota y del veterinario, o vacío si no existe
     */
    Optional<ClinicalAttention> findById(int atencionId);
}
