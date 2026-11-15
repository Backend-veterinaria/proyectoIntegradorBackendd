package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DataDetailAppointmentDTO {
    @NotNull(message = "required field idAppointment")
    @Positive(message = "idAppointment must be positive")
    private final Integer idAppointment;
    @NotNull(message = "required field idClient")
    @Positive(message = "idClient must be positive")
    private final Integer idClient;
    @NotBlank(message = "required field username")
    private final String username;
}
