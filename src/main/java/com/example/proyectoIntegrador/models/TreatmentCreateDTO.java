package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Tratamiento formulado a una mascota (HC-04, RF-11).
 */
@Data
public class TreatmentCreateDTO {

    @NotNull(message = "required field mascotaId")
    @Positive(message = "mascotaId must be positive")
    private Integer mascotaId;

    /** Opcional: atención clínica en la que se formuló (debe ser de la misma mascota). */
    @Positive(message = "atencionId must be positive")
    private Integer atencionId;

    @NotBlank(message = "required field medicamento")
    @Size(max = 200, message = "medicamento max 200 characters")
    private String medicamento;

    @NotBlank(message = "required field dosis")
    @Size(max = 200, message = "dosis max 200 characters")
    private String dosis;

    @Size(max = 200, message = "frecuencia max 200 characters")
    private String frecuencia;

    @Size(max = 4000, message = "indicaciones max 4000 characters")
    private String indicaciones;

    @NotBlank(message = "required field fechaInicio")
    @Pattern(regexp = "[0-9]{8}", message = "fechaInicio must have format yyyyMMdd")
    private String fechaInicio;

    /** Opcional: fecha prevista de fin. */
    @Pattern(regexp = "[0-9]{8}", message = "fechaFin must have format yyyyMMdd")
    private String fechaFin;
}
