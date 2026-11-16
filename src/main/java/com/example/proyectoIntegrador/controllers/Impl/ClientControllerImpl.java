package com.example.proyectoIntegrador.controllers.Impl;

import com.example.proyectoIntegrador.controllers.ClientController;
import com.example.proyectoIntegrador.enums.CodesResponse;
import com.example.proyectoIntegrador.exceptions.ErrorMessages;
import com.example.proyectoIntegrador.exceptions.UserAlreadyExistsException;
import com.example.proyectoIntegrador.models.ClientCreateDTO;
import com.example.proyectoIntegrador.models.ClientUpdateDTO;
import com.example.proyectoIntegrador.models.PasswordChangeDTO;
import com.example.proyectoIntegrador.models.ResponseGeneric;
import com.example.proyectoIntegrador.models.StatusDTO;
import com.example.proyectoIntegrador.security.SecurityUtils;
import com.example.proyectoIntegrador.services.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ClientControllerImpl implements ClientController {

    private final ClientService clientService;

    @Override
    public ResponseEntity<ResponseGeneric> list(boolean includeInactive, String q) {
        log.debug("START list clients");
        return handle(CodesResponse.ERROR_GET_DATA, () -> clientService.list(includeInactive, q));
    }

    @Override
    public ResponseEntity<ResponseGeneric> get(int clienteId) {
        log.debug("START get client {}", clienteId);
        return handle(CodesResponse.ERROR_GET_DATA, () -> clientService.get(SecurityUtils.currentUser(), clienteId));
    }

    @Override
    public ResponseEntity<ResponseGeneric> update(int clienteId, ClientUpdateDTO dto) {
        log.debug("START update client {}", clienteId);
        return handle(CodesResponse.ERROR_MODIFY_DATA, () -> clientService.update(SecurityUtils.currentUser(), clienteId, dto));
    }

    @Override
    public ResponseEntity<ResponseGeneric> changeStatus(int clienteId, StatusDTO dto) {
        log.debug("START changeStatus client {}", clienteId);
        return handle(CodesResponse.ERROR_MODIFY_DATA,
                () -> clientService.changeStatus(SecurityUtils.currentUser(), clienteId, dto.getActivo()));
    }

    @Override
    public ResponseEntity<ResponseGeneric> create(ClientCreateDTO dto) {
        log.debug("START create client");
        try {
            ResponseGeneric response = new ResponseGeneric();
            response.setResponseObj(clientService.create(SecurityUtils.currentUser(), dto));
            response.setResponseCode(CodesResponse.OK.getCode());
            response.setResponseDesc(CodesResponse.OK.getDescription());
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            // Se deja pasar para que GlobalExceptionHandler responda 403
            throw e;
        } catch (UserAlreadyExistsException e) {
            // Mismo código que el registro público (9994), para que el frontend lo maneje igual
            return error(CodesResponse.USER_EXISTS, e);
        } catch (Exception e) {
            return error(CodesResponse.ERROR_SAVE_DATA, e);
        }
    }

    @Override
    public ResponseEntity<ResponseGeneric> resetPassword(int clienteId) {
        log.debug("START resetPassword client {}", clienteId);
        return handle(CodesResponse.ERROR_MODIFY_DATA,
                () -> clientService.resetPassword(SecurityUtils.currentUser(), clienteId));
    }

    @Override
    public ResponseEntity<ResponseGeneric> changePassword(int clienteId, PasswordChangeDTO dto) {
        log.debug("START changePassword client {}", clienteId);
        return handle(CodesResponse.ERROR_MODIFY_DATA, () -> {
            clientService.changePassword(SecurityUtils.currentUser(), clienteId, dto);
            return "Contraseña actualizada";
        });
    }

    private static ResponseEntity<ResponseGeneric> error(CodesResponse code, Exception e) {
        ResponseGeneric response = new ResponseGeneric();
        response.setResponseCode(code.getCode());
        response.setResponseDesc(code.getDescription());
        response.setResponseObj(ErrorMessages.clientMessage(e));
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<ResponseGeneric> handle(CodesResponse errorCode, Supplier<Object> action) {
        try {
            ResponseGeneric response = new ResponseGeneric();
            response.setResponseObj(action.get());
            response.setResponseCode(CodesResponse.OK.getCode());
            response.setResponseDesc(CodesResponse.OK.getDescription());
            return ResponseEntity.ok(response);

        } catch (AccessDeniedException e) {
            // Se deja pasar para que GlobalExceptionHandler responda 403
            throw e;
        } catch (Exception e) {
            ResponseGeneric responseError = new ResponseGeneric();
            responseError.setResponseCode(errorCode.getCode());
            responseError.setResponseDesc(errorCode.getDescription());
            responseError.setResponseObj(ErrorMessages.clientMessage(e));
            return ResponseEntity.ok(responseError);
        }
    }
}
