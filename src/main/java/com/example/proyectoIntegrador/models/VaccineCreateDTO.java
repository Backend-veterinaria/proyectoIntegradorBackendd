package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Vacuna aplicada a una mascota (HC-05, RF-11).
 */
@Data
public class VaccineCreateDTO {

    @NotNull(message = "required field mascotaId")
    @Positive(message = "mascotaId must be positive")
    private Integer mascotaId;

    /** Opcional: atención clínica en la que se aplicó (debe ser de la misma mascota). */
    @Positive(message = "atencionId must be positive")
    private Integer atencionId;

    @NotBlank(message = "required field nombre")
    @Size(max = 200, message = "nombre max 200 characters")
    private String nombre;

    @Size(max = 100, message = "lote max 100 characters")
    private String lote;

    @NotBlank(message = "required field fechaAplicacion")
    @Pattern(regexp = "[0-9]{8}", message = "fechaAplicacion must have format yyyyMMdd")
    private String fechaAplicacion;

    /** Opcional: fecha de la siguiente dosis o refuerzo. */
    @Pattern(regexp = "[0-9]{8}", message = "proximaDosis must have format yyyyMMdd")
    private String proximaDosis;

    @Size(max = 4000, message = "observaciones max 4000 characters")
    private String observaciones;
}
