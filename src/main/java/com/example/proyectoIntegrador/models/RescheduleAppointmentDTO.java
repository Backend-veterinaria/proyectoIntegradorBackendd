package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Nueva fecha y hora de una cita (VET-04). El veterinario es opcional: si no se envía, se conserva.
 */
@Data
public class RescheduleAppointmentDTO {
    @NotBlank(message = "required field fecha")
    @Pattern(regexp = "[0-9]{8}", message = "fecha must have format yyyyMMdd")
    private String fecha;
    @NotBlank(message = "required field hora")
    @Pattern(regexp = "[0-9]{6}", message = "hora must have format HHmmss")
    private String hora;
    @Positive(message = "medicoId must be positive")
    private Integer medicoId;
}
