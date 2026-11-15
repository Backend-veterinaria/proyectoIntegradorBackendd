package com.example.proyectoIntegrador.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Datos editables de un veterinario (VET-01). El username y la contraseña no se cambian aquí.
 */
@Data
public class VeterinarianUpdateDTO {
    @NotBlank(message = "required field nombre")
    private String nombre;
    @NotBlank(message = "required field apellido")
    private String apellido;
    @NotBlank(message = "required field telefono")
    private String telefono;
    /** Opcional: si no se envía, se conserva la dirección actual. */
    private String direccion;
    @NotBlank(message = "required field email")
    @Email(message = "email is not valid")
    private String email;
}
