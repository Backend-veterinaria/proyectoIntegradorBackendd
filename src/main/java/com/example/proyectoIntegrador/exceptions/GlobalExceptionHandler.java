package com.example.proyectoIntegrador.exceptions;

import com.example.proyectoIntegrador.enums.CodesResponse;
import com.example.proyectoIntegrador.models.ResponseGeneric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Manejo centralizado de errores (QA-01, RNF-05). Todas las respuestas usan la estructura
 * ResponseGeneric:
 * <ul>
 *   <li>Datos de entrada inválidos (validación, JSON mal formado, tipo de parámetro, parámetro
 *       faltante): HTTP 200, código 6617 y la lista de campos con error.</li>
 *   <li>Sin permiso: HTTP 403, código 6619. Sin token o vencido: HTTP 401, código 6618
 *       (lo escribe SecurityConfig, porque ocurre antes de llegar a Spring MVC).</li>
 *   <li>Cualquier otro error no capturado: código 6620 y un mensaje genérico; el detalle solo
 *       queda en el log del servidor.</li>
 * </ul>
 * Los errores de negocio los capturan los propios controladores (ErrorMessages.clientMessage).
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
     * Parámetro de la ruta o de la URL con un tipo que no corresponde (por ejemplo /pets/abc o
     * ?days=diez). Es un error del cliente, no del servidor: no debe llegar al catch-all como 500.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResponseGeneric> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.debug("Parametro con tipo invalido: {}={}", e.getName(), e.getValue());
        return ResponseEntity.ok(buildError(List.of(e.getName() + ": invalid value")));
    }

    /**
     * Falta un parámetro obligatorio de la URL (@RequestParam sin valor por defecto).
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ResponseGeneric> handleMissingParameter(MissingServletRequestParameterException e) {
        log.debug("Falta el parametro {}", e.getParameterName());
        return ResponseEntity.ok(buildError(List.of(e.getParameterName() + ": required parameter")));
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

    /**
     * Cualquier excepción que no capturó el controlador. Se registra completa en el log y al
     * cliente solo se le envía un mensaje genérico. Las excepciones propias de Spring MVC
     * (ruta inexistente, método no permitido, etc.) conservan su código HTTP.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseGeneric> handleUnexpected(Exception e) {
        HttpStatusCode status = e instanceof ErrorResponse errorResponse
                ? errorResponse.getStatusCode()
                : HttpStatus.INTERNAL_SERVER_ERROR;

        if (status.is5xxServerError()) {
            log.error("Error no controlado", e);
        } else {
            log.debug("Peticion rechazada: {}", e.getMessage());
        }

        ResponseGeneric response = new ResponseGeneric();
        response.setResponseCode(CodesResponse.ERROR_UNEXPECTED.getCode());
        response.setResponseDesc(CodesResponse.ERROR_UNEXPECTED.getDescription());
        response.setResponseObj(ErrorMessages.GENERIC_MESSAGE);
        return ResponseEntity.status(status).body(response);
    }

    private ResponseGeneric buildError(List<String> errors) {
        ResponseGeneric response = new ResponseGeneric();
        response.setResponseCode(CodesResponse.ERROR_VALIDATION.getCode());
        response.setResponseDesc(CodesResponse.ERROR_VALIDATION.getDescription());
        response.setResponseObj(errors);
        return response;
    }
}
