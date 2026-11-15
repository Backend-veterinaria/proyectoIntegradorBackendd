package com.example.proyectoIntegrador.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseLogin extends ResponseGeneric{

    /** Token JWT que el cliente debe enviar en el encabezado Authorization: Bearer <token>. */
    private String token;
    /** Segundos de validez del token. */
    private Long expiresIn;

}
