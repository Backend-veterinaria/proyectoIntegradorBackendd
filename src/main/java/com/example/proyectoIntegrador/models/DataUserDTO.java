package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DataUserDTO {
    @NotNull(message = "required field idClient")
    @Positive(message = "idClient must be positive")
    private Integer idClient;
}
