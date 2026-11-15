package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Parámetros para consultar la agenda de un veterinario en un día (VET-02, RF-07).
 */
@Data
public class DataVeterinarianAgendaDTO {
    @NotBlank(message = "required field fecha")
    @Pattern(regexp = "[0-9]{8}", message = "fecha must have format yyyyMMdd")
    private String fecha;
    /** Obligatorio para el administrador. El veterinario puede omitirlo: se usa el suyo. */
    @Positive(message = "medicoId must be positive")
    private Integer medicoId;
    /** Opcional: filtra por estado de la cita. */
    @Positive(message = "estadoId must be positive")
    private Integer estadoId;
}
