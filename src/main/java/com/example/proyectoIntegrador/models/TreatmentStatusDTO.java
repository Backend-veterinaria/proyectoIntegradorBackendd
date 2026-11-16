package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Seguimiento de un tratamiento (HC-04): finalizarlo o suspenderlo.
 */
@Data
public class TreatmentStatusDTO {

    @NotBlank(message = "required field estado")
    @Pattern(regexp = "FINALIZADO|SUSPENDIDO", message = "estado must be FINALIZADO or SUSPENDIDO")
    private String estado;
}
