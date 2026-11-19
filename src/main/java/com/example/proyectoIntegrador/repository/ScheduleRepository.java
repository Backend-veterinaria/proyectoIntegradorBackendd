package com.example.proyectoIntegrador.repository;

import com.example.proyectoIntegrador.models.ScheduleModels.SystemParameter;
import com.example.proyectoIntegrador.models.ScheduleModels.TimeSlotRange;

import java.util.List;
import java.util.Optional;

/**
 * Horarios de atención y parámetros del sistema (CIT-08). Requiere sql/CIT-08_horarios_atencion.sql.
 */
public interface ScheduleRepository {

    /**
     * Indica si hay al menos una franja activa. Si no hay (o no existe la tabla porque no se ejecutó el
     * script) el sistema no valida horarios. Atrapa las excepciones y devuelve false.
     */
    boolean hasAnySchedule();

    /**
     * Franjas activas de un día: las del veterinario (medicoId) y las generales (medico_id NULL).
     * Cada franja trae en diaSemana el valor; el servicio decide cuáles aplican. No captura excepciones.
     *
     * @return pares (medicoId o null, franja)
     */
    List<OwnedRange> findDay(int medicoId, int diaSemana);

    /** Franjas activas exactas de la clínica (medicoId null) o de un veterinario, por día y hora. */
    List<TimeSlotRange> findWeek(Integer medicoId);

    /** Borra las franjas de la clínica o del veterinario e inserta las nuevas. Llamar en una transacción. */
    void replaceWeek(Integer medicoId, List<TimeSlotRange> franjas);

    List<SystemParameter> findParameters();

    Optional<String> findParameter(String clave);

    /** @return filas actualizadas (0 si la clave no existe) */
    int updateParameter(String clave, String valor);

    /** Franja con el veterinario al que pertenece (null = general de la clínica). */
    record OwnedRange(Integer medicoId, TimeSlotRange range) {
    }
}
