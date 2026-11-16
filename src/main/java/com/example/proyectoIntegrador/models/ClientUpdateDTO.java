package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Datos editables de un propietario (CIT-03). El username y la contraseña no se cambian aquí.
 */
@Data
public class ClientUpdateDTO {
    @NotBlank(message = "required field nombre")
    private String nombre;
    @NotBlank(message = "required field apellido")
    private String apellido;
    @NotBlank(message = "required field direccion")
    private String direccion;
    @NotBlank(message = "required field telefono")
    private String telefono;
    @NotBlank(message = "required field email")
    @Email(message = "email is not valid")
    private String email;
}
