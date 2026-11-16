package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Propietario registrado por el personal de la clínica (CIT-06, RF-03). No lleva contraseña: el
 * sistema genera una temporal que el tutor cambia en su primer inicio de sesión.
 */
@Data
public class ClientCreateDTO {

    @NotBlank(message = "required field nombre")
    private String nombre;

    @NotBlank(message = "required field apellido")
    private String apellido;

    /** Opcional: puede no conocerse al registrarlo en la clínica. */
    private String direccion;

    @NotBlank(message = "required field telefono")
    private String telefono;

    @NotBlank(message = "required field email")
    @Email(message = "email is not valid")
    private String email;

    @NotBlank(message = "required field username")
    @Size(min = 3, max = 50, message = "username must have between 3 and 50 characters")
    private String username;
}
