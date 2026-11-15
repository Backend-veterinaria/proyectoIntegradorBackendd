package com.example.proyectoIntegrador.exceptions;

import com.example.proyectoIntegrador.enums.CodesResponse;
import com.example.proyectoIntegrador.models.ResponseGeneric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Convierte los errores de validación de entrada (que ocurren antes de entrar al controlador)
 * en la misma estructura ResponseGeneric que usan todos los servicios, con HTTP 200 y
 * el código ERROR_VALIDATION, para que el cliente los maneje igual que los demás errores.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Falla de @Valid en @RequestBody (MethodArgumentNotValidException) y en @ModelAttribute (BindException).
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ResponseGeneric> handleValidation(BindException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        log.debug("Error de validacion: {}", errors);
        return ResponseEntity.ok(buildError(errors));
    }

    /**
     * Cuerpo JSON ausente o mal formado (por ejemplo, texto en un campo numérico).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseGeneric> handleUnreadable(HttpMessageNotReadableException e) {
        log.debug("Cuerpo de la peticion no legible: {}", e.getMessage());
        return ResponseEntity.ok(buildError(List.of("El cuerpo de la peticion no es un JSON valido")));
    }

    /**
     * Un usuario autenticado intenta acceder a datos de otro cliente (SecurityUtils).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseGeneric> handleAccessDenied(AccessDeniedException e) {
        log.debug("Acceso denegado: {}", e.getMessage());
        ResponseGeneric response = new ResponseGeneric();
        response.setResponseCode(CodesResponse.ERROR_FORBIDDEN.getCode());
        response.setResponseDesc(CodesResponse.ERROR_FORBIDDEN.getDescription());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    private ResponseGeneric buildError(List<String> errors) {
        ResponseGeneric response = new ResponseGeneric();
        response.setResponseCode(CodesResponse.ERROR_VALIDATION.getCode());
        response.setResponseDesc(CodesResponse.ERROR_VALIDATION.getDescription());
        response.setResponseObj(errors);
        return response;
    }
}
