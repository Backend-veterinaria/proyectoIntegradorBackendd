package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Filtro de los reportes (REP-01..03): período (ambas fechas incluidas) y, opcionalmente, un veterinario.
 */
@Data
public class ReportFilterDTO {

    @NotBlank(message = "required field desde")
    @Pattern(regexp = "[0-9]{8}", message = "desde must have format yyyyMMdd")
    private String desde;

    @NotBlank(message = "required field hasta")
    @Pattern(regexp = "[0-9]{8}", message = "hasta must have format yyyyMMdd")
    private String hasta;

    /** Opcional para el administrador; el veterinario siempre ve solo sus datos. */
    @Positive(message = "medicoId must be positive")
    private Integer medicoId;
}
