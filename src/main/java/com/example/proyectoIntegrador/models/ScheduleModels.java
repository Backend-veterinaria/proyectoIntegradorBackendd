package com.example.proyectoIntegrador.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Horarios de atención y parámetros del sistema (CIT-08).
 */
public final class ScheduleModels {

    private ScheduleModels() {
    }

    /** Una franja de atención. Horas en formato HH:mm. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlotRange {
        /** 1 = lunes ... 7 = domingo. */
        @NotNull(message = "required field diaSemana")
        @Min(value = 1, message = "diaSemana must be between 1 and 7")
        @Max(value = 7, message = "diaSemana must be between 1 and 7")
        private Integer diaSemana;

        @NotBlank(message = "required field horaInicio")
        @Pattern(regexp = "([01][0-9]|2[0-3]):[0-5][0-9]", message = "horaInicio must have format HH:mm")
        private String horaInicio;

        @NotBlank(message = "required field horaFin")
        @Pattern(regexp = "([01][0-9]|2[0-3]):[0-5][0-9]", message = "horaFin must have format HH:mm")
        private String horaFin;
    }

    /** Semana de atención de la clínica (medicoId null) o de un veterinario. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeekSchedule {
        private Integer medicoId;
        /** true si el veterinario no tiene horario propio y se le aplica el de la clínica. */
        private boolean usaHorarioGeneral;
        private List<TimeSlotRange> franjas;
    }

    /** Cuerpo de PUT /schedules: reemplaza la semana completa. Una lista vacía deja el horario sin franjas. */
    @Data
    public static class WeekScheduleDTO {
        @NotNull(message = "required field franjas")
        private List<@Valid TimeSlotRange> franjas;
    }

    /** Horas libres de un veterinario en un día. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Availability {
        private String fecha;
        private Integer medicoId;
        private int duracionMinutos;
        /** Horas de inicio libres, en formato HH:mm, ordenadas. */
        private List<String> horarios;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemParameter {
        private String clave;
        private String valor;
        private String descripcion;
    }

    @Data
    public static class ParameterValueDTO {
        @NotBlank(message = "required field valor")
        private String valor;
    }
}
