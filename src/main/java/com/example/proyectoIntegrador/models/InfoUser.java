package com.example.proyectoIntegrador.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InfoUser {
//    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String idClient;
    private String name;
    private String lastName;
    private String addres;
    private String phone;
    private String email;
    private String username;
    /** ADMIN, VETERINARIO o CLIENTE. */
    private String role;
    /** true si inició sesión con una contraseña temporal y debe elegir una nueva (CIT-06). */
    private Boolean cambiarPassword;
}
