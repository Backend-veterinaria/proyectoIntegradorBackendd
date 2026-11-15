package com.example.proyectoIntegrador.exceptions;

/**
 * Se lanza al intentar registrar un usuario cuyo username o email ya está registrado.
 */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
