package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * Datos editables de una mascota (CIT-02). El dueño no se cambia aquí.
 */
@Data
public class PetUpdateDTO {
    @NotBlank(message = "required field nombre")
    private String nombre;
    @NotNull(message = "required field edad")
    @PositiveOrZero(message = "edad must be zero or positive")
    private Integer edad;
    @NotBlank(message = "required field genero")
    private String genero;
    @NotNull(message = "required field razaId")
    @Positive(message = "razaId must be positive")
    private Integer razaId;
}
