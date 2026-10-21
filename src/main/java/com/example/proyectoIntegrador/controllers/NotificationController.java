package com.example.proyectoIntegrador.controllers;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/integrador/pets")
public interface NotificationController {

    @GetMapping(value = "/notifications/connection",   produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter notify(String client_id);
}
