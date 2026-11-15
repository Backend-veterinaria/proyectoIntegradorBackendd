package com.example.proyectoIntegrador.controllers.Impl;

import com.example.proyectoIntegrador.controllers.NotificationController;
import com.example.proyectoIntegrador.services.NotificationService;
import com.example.proyectoIntegrador.security.SecurityUtils;
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
        // El cliente solo puede abrir su propio canal de notificaciones. client_id es opcional:
        // si no se envía, se usa el id del token.
        int userId = SecurityUtils.currentUser().userId();
        if (client_id != null && !client_id.equals(String.valueOf(userId))) {
            SecurityUtils.requireSameClient(-1);
        }
        return notificationService.stablishConnection(String.valueOf(userId));
    }
}
