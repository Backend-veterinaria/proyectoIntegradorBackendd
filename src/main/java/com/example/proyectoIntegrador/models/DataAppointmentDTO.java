package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DataAppointmentDTO {
    @NotNull(message = "required field estadoId")
    @Positive(message = "estadoId must be positive")
    private Integer estadoId;
    @NotNull(message = "required field clienteId")
    @Positive(message = "clienteId must be positive")
    private Integer clienteId;
    @NotNull(message = "required field medicoId")
    @Positive(message = "medicoId must be positive")
    private Integer medicoId;
    @NotNull(message = "required field mascotaId")
    @Positive(message = "mascotaId must be positive")
    private Integer mascotaId;
    @NotBlank(message = "required field description")
    private String description;
    @NotBlank(message = "required field fecha")
    @Pattern(regexp = "[0-9]{8}", message = "fecha must have format yyyyMMdd")
    private String fecha;
    @NotBlank(message = "required field hora")
    @Pattern(regexp = "[0-9]{6}", message = "hora must have format HHmmss")
    private String hora;
}
