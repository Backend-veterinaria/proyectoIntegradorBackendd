package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.models.ScheduleModels.Availability;
import com.example.proyectoIntegrador.models.ScheduleModels.SystemParameter;
import com.example.proyectoIntegrador.models.ScheduleModels.WeekSchedule;
import com.example.proyectoIntegrador.models.ScheduleModels.WeekScheduleDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Horarios de atención y parámetros del sistema, configurables desde la base de datos (CIT-08).
 */
public interface ScheduleService {

    /** Duración de una cita en minutos (parametro_sistema.duracion_cita_minutos; 30 si no está configurada). */
    int appointmentMinutes();

    /**
     * Rechaza una hora fuera del horario del veterinario. Si no hay ningún horario configurado no valida
     * nada, para no bloquear el agendamiento mientras no se haya ejecutado sql/CIT-08.
     *
     * @throws com.example.proyectoIntegrador.exceptions.BusinessException si el veterinario no atiende a esa hora
     */
    void requireWithinSchedule(int medicoId, LocalDate fecha, LocalTime hora, int minutes);

    /** Horas libres del veterinario ese día (fecha yyyyMMdd). */
    Availability availability(int medicoId, String fecha);

    /** Horario semanal de la clínica (medicoId null) o de un veterinario. */
    WeekSchedule getWeek(Integer medicoId);

    /** Reemplaza el horario semanal de la clínica (medicoId null) o de un veterinario. */
    WeekSchedule replaceWeek(Integer medicoId, WeekScheduleDTO dto);

    List<SystemParameter> parameters();

    SystemParameter updateParameter(String clave, String valor);
}
