package com.example.proyectoIntegrador.enums;

import lombok.Getter;

@Getter
public enum CodesResponse {

    OK("0000", "STATUS OK"),
    ERROR_NOT_LOGIN("1231", "Error. La contraseña o el usuario son incorrectos"),
    USER_EXISTS("9994", "User already exists"),
    ERROR_REGISTER_USER("6611", "Error al registrar usuario"),
    ERROR_SAVE_AGEND("6612", "Error. No se pudo agendar la cita"),
    ERROR_GET_DATA("6615", "Error. No se pudo consultar el registro"),
    ERROR_MODIFY_DATA("6616", "Error. No se pudo modificar el registro"),
    ERROR_VALIDATION("6617", "Error. Los datos enviados no son validos"),
    ERROR_UNAUTHORIZED("6618", "Error. Debe iniciar sesion o el token no es valido"),
    ERROR_FORBIDDEN("6619", "Error. No tiene permiso para acceder a este recurso"),
    ERROR_UNEXPECTED("6620", "Error. Ocurrio un error inesperado"),
    ERROR_SAVE_DATA("6621", "Error. No se pudo guardar el registro"),
    ;


    private final String code;
    private final String description;

    private CodesResponse(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
