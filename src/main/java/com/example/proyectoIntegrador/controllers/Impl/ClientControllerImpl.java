package com.example.proyectoIntegrador.controllers.Impl;

import com.example.proyectoIntegrador.controllers.ClientController;
import com.example.proyectoIntegrador.enums.CodesResponse;
import com.example.proyectoIntegrador.exceptions.ErrorMessages;
import com.example.proyectoIntegrador.models.ClientUpdateDTO;
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
