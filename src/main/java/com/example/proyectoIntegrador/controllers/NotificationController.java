package com.example.proyectoIntegrador.controllers;

import com.example.proyectoIntegrador.models.NotificationModels.PreferencesDTO;
import com.example.proyectoIntegrador.models.ResponseGeneric;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Notificaciones: canal SSE en tiempo real, bandeja de avisos guardados y preferencias (CIT-09). Cada
 * usuario solo ve y configura las suyas (el usuario sale del token).
 */
@RestController
@RequestMapping("/integrador")
@Tag(name = "Notificaciones", description = "Canal SSE en tiempo real (token en ?token=), avisos guardados y preferencias")
public interface NotificationController {

    @GetMapping(value = "/notifications/connection", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter notify(@RequestParam(name = "client_id", required = false) String client_id);

    /** Avisos guardados, del más reciente al más antiguo, con el total de no leídos. */
    @GetMapping("/notifications")
    ResponseEntity<ResponseGeneric> inbox(@RequestParam(name = "soloNoLeidas", defaultValue = "false") boolean soloNoLeidas,
                                          @RequestParam(name = "limit", defaultValue = "50") int limit);

    @PatchMapping("/notifications/{notificacionId}/read")
    ResponseEntity<ResponseGeneric> markRead(@PathVariable("notificacionId") int notificacionId);

    @PatchMapping("/notifications/read-all")
    ResponseEntity<ResponseGeneric> markAllRead();

    /** Tipos de aviso con lo que eligió el usuario (activa=true si nunca lo cambió). */
    @GetMapping("/notifications/preferences")
    ResponseEntity<ResponseGeneric> preferences();

    /** Activa o desactiva tipos de aviso. Solo se envían los que cambian. */
    @PutMapping("/notifications/preferences")
    ResponseEntity<ResponseGeneric> updatePreferences(@Valid @RequestBody PreferencesDTO dto);
}
