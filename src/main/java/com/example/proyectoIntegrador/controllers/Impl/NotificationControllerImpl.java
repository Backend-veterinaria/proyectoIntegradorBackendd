package com.example.proyectoIntegrador.controllers.Impl;

import com.example.proyectoIntegrador.controllers.NotificationController;
import com.example.proyectoIntegrador.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RequiredArgsConstructor
@RestController
public class NotificationControllerImpl implements NotificationController {

    private final NotificationService notificationService;

    @Override
    public SseEmitter notify(String client_id) {
        return notificationService.stablishConnection(client_id);
    }
}
