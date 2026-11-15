package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DataCancelledAppointmentDTO {
    @NotNull(message = "required field idCliente")
    @Positive(message = "idCliente must be positive")
    private final Integer idCliente;
    @NotNull(message = "required field idAppointment")
    @Positive(message = "idAppointment must be positive")
    private final Integer idAppointment;
}
