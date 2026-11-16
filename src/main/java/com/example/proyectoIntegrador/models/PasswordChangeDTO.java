package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Cambio de contraseña por el propio usuario (CIT-06).
 */
@Data
public class PasswordChangeDTO {

    @NotBlank(message = "required field passwordActual")
    private String passwordActual;

    @NotBlank(message = "required field passwordNueva")
    @Size(min = 8, max = 72, message = "passwordNueva must have between 8 and 72 characters")
    private String passwordNueva;
}
