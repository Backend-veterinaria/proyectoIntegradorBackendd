package com.example.proyectoIntegrador.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RequestMapping("/integrador")
@Tag(name = "Notificaciones", description = "Canal SSE de notificaciones en tiempo real (token en ?token=)")
public interface NotificationController {

    @GetMapping(value = "/notifications/connection", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter notify(@RequestParam(name = "client_id", required = false) String client_id);
}
