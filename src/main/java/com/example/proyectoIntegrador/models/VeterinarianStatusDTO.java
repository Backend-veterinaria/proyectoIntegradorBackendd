package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Activar o inactivar un veterinario (VET-01, CPA-09).
 */
@Data
public class VeterinarianStatusDTO {
    @NotNull(message = "required field activo")
    private Boolean activo;
}
