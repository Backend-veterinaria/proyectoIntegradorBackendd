package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class DataMascotaDTO {
    @NotNull(message = "required field idCliente")
    @Positive(message = "idCliente must be positive")
    private final Integer idCliente;
    @NotBlank(message = "required field nombre")
    private final String nombre;
    @NotNull(message = "required field edad")
    @PositiveOrZero(message = "edad must be zero or positive")
    private final Integer edad;
    @NotBlank(message = "required field genero")
    private final String genero;
    @NotNull(message = "required field razaId")
    @Positive(message = "razaId must be positive")
    private final Integer razaId;
}
