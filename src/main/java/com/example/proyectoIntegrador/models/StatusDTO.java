package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Activar o inactivar un registro ({"activo": false} para inactivar).
 */
@Data
public class StatusDTO {
    @NotNull(message = "required field activo")
    private Boolean activo;
}
