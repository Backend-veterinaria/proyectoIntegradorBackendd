package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Nuevo estado de una cita (VET-03). El estadoId es el de la tabla estado_cita
 * (se obtiene del selector de estados).
 */
@Data
public class AppointmentStatusDTO {
    @NotNull(message = "required field estadoId")
    @Positive(message = "estadoId must be positive")
    private Integer estadoId;
}
