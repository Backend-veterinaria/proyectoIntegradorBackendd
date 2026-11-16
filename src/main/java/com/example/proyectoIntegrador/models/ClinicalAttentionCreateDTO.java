package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Atención clínica de una cita (HC-01, RF-09). La mascota y el veterinario se toman de la cita.
 */
@Data
public class ClinicalAttentionCreateDTO {

    @NotNull(message = "required field agendaId")
    @Positive(message = "agendaId must be positive")
    private Integer agendaId;

    @Size(max = 500, message = "motivoConsulta max 500 characters")
    private String motivoConsulta;

    @NotBlank(message = "required field diagnostico")
    @Size(max = 4000, message = "diagnostico max 4000 characters")
    private String diagnostico;

    @Size(max = 4000, message = "procedimientos max 4000 characters")
    private String procedimientos;

    @Size(max = 4000, message = "observaciones max 4000 characters")
    private String observaciones;

    @DecimalMin(value = "0.01", message = "pesoKg must be positive")
    @DecimalMax(value = "9999.99", message = "pesoKg is too large")
    private BigDecimal pesoKg;

    @DecimalMin(value = "30.0", message = "temperaturaC must be between 30 and 45")
    @DecimalMax(value = "45.0", message = "temperaturaC must be between 30 and 45")
    private BigDecimal temperaturaC;
}
