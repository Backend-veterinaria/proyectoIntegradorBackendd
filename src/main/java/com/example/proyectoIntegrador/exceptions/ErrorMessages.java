package com.example.proyectoIntegrador.exceptions;

import lombok.extern.slf4j.Slf4j;

/**
 * Decide qué mensaje de error se puede enviar al cliente (RNF-05: los errores no exponen
 * detalles internos del sistema).
 */
@Slf4j
public final class ErrorMessages {

    public static final String GENERIC_MESSAGE = "Ocurrio un error inesperado. Intente de nuevo mas tarde";

    private ErrorMessages() {
    }

    /**
     * @return el mensaje de la excepción si es un error de negocio; si no, registra el error
     * completo en el log del servidor y devuelve un mensaje genérico
     */
    public static String clientMessage(Exception e) {
        if (e instanceof BusinessException) {
            return e.getMessage();
        }
        log.error("Error inesperado", e);
        return GENERIC_MESSAGE;
    }
}
