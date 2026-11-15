package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Datos para crear un veterinario junto con su usuario para iniciar sesión (VET-01).
 */
@Data
public class VeterinarianCreateDTO {
    @NotBlank(message = "required field nombre")
    private String nombre;
    @NotBlank(message = "required field apellido")
    private String apellido;
    @NotBlank(message = "required field telefono")
    private String telefono;
    /** Opcional. */
    private String direccion;
    @NotBlank(message = "required field email")
    @Email(message = "email is not valid")
    private String email;
    @NotBlank(message = "required field username")
    @Size(min = 3, max = 50, message = "username must have between 3 and 50 characters")
    private String username;
    @NotBlank(message = "required field password")
    @Size(min = 8, max = 72, message = "password must have between 8 and 72 characters")
    private String password;
}
