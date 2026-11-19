package com.example.proyectoIntegrador.controllers.Impl;

import com.example.proyectoIntegrador.controllers.NotificationController;
import com.example.proyectoIntegrador.enums.CodesResponse;
import com.example.proyectoIntegrador.exceptions.ErrorMessages;
import com.example.proyectoIntegrador.models.NotificationModels.PreferencesDTO;
import com.example.proyectoIntegrador.models.ResponseGeneric;
import com.example.proyectoIntegrador.services.NotificationService;
import org.springframework.http.ResponseEntity;

import java.util.function.Supplier;
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
            SecurityUtils.requireOwnClient(-1);
        }
        return notificationService.stablishConnection(String.valueOf(userId));
    }

    @Override
    public ResponseEntity<ResponseGeneric> inbox(boolean soloNoLeidas, int limit) {
        return handle(CodesResponse.ERROR_GET_DATA,
                () -> notificationService.inbox(SecurityUtils.currentUser().userId(), soloNoLeidas, limit));
    }

    @Override
    public ResponseEntity<ResponseGeneric> markRead(int notificacionId) {
        return handle(CodesResponse.ERROR_MODIFY_DATA, () -> {
            notificationService.markRead(SecurityUtils.currentUser().userId(), notificacionId);
            return true;
        });
    }

    @Override
    public ResponseEntity<ResponseGeneric> markAllRead() {
        return handle(CodesResponse.ERROR_MODIFY_DATA, () -> {
            notificationService.markAllRead(SecurityUtils.currentUser().userId());
            return true;
        });
    }

    @Override
    public ResponseEntity<ResponseGeneric> preferences() {
        return handle(CodesResponse.ERROR_GET_DATA,
                () -> notificationService.preferences(SecurityUtils.currentUser().userId()));
    }

    @Override
    public ResponseEntity<ResponseGeneric> updatePreferences(PreferencesDTO dto) {
        return handle(CodesResponse.ERROR_MODIFY_DATA,
                () -> notificationService.updatePreferences(SecurityUtils.currentUser().userId(), dto.getPreferencias()));
    }

    private ResponseEntity<ResponseGeneric> handle(CodesResponse errorCode, Supplier<Object> action) {
        ResponseGeneric response = new ResponseGeneric();
        try {
            response.setResponseObj(action.get());
            response.setResponseCode(CodesResponse.OK.getCode());
            response.setResponseDesc(CodesResponse.OK.getDescription());
        } catch (Exception e) {
            response.setResponseCode(errorCode.getCode());
            response.setResponseDesc(errorCode.getDescription());
            response.setResponseObj(ErrorMessages.clientMessage(e));
        }
        return ResponseEntity.ok(response);
    }
}
