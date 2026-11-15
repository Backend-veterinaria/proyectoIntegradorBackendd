package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterDTO {
    @NotBlank(message = "you must specify the nombre field")
    private String nombre;
    @NotBlank(message = "you must specify the apellido field")
    private String apellido;
    @NotBlank(message = "you must specify the direccion field")
    private String direccion;
    @NotBlank(message = "you must specify the telefono field")
    private String telefono;
    @NotBlank(message = "you must specify the email field")
    @Email(message = "email is not valid")
    private String email;
    private LocalDate fechaRegistro;
    @NotBlank(message = "you must specify the username field")
    @Size(min = 3, max = 50, message = "username must have between 3 and 50 characters")
    private String username;
    @NotBlank(message = "you must specify the password field")
    // max 72: límite de BCrypt (se usará al cifrar las contraseñas)
    @Size(min = 8, max = 72, message = "password must have between 8 and 72 characters")
    private String password;
}
