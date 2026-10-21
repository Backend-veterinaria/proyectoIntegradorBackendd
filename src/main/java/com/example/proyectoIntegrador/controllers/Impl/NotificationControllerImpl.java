package com.example.proyectoIntegrador.controllers.Impl;

import com.example.proyectoIntegrador.controllers.NotificationController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class NotificationControllerImpl implements NotificationController {

    @Override
    public SseEmitter notify(String client_id) {

        return null;
    }
}
