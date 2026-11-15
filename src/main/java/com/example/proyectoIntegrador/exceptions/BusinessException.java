package com.example.proyectoIntegrador.exceptions;

/**
 * Error de negocio cuyo mensaje está pensado para mostrarse al usuario
 * (por ejemplo, "No hay disponibilidad de citas").
 * <p>
 * Cualquier otra excepción se considera interna: su mensaje puede contener detalles técnicos
 * (SQL, nombres de tablas, trazas) y nunca se envía al cliente (RNF-05). Ver {@link ErrorMessages}.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
