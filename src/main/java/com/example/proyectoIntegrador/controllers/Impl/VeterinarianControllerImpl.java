package com.example.proyectoIntegrador.controllers.Impl;

import com.example.proyectoIntegrador.controllers.VeterinarianController;
import com.example.proyectoIntegrador.enums.CodesResponse;
import com.example.proyectoIntegrador.exceptions.ErrorMessages;
import com.example.proyectoIntegrador.exceptions.UserAlreadyExistsException;
import com.example.proyectoIntegrador.models.ResponseGeneric;
import com.example.proyectoIntegrador.models.VeterinarianCreateDTO;
import com.example.proyectoIntegrador.models.VeterinarianStatusDTO;
import com.example.proyectoIntegrador.models.VeterinarianUpdateDTO;
import com.example.proyectoIntegrador.services.VeterinarianService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@Slf4j
@RestController
@RequiredArgsConstructor
public class VeterinarianControllerImpl implements VeterinarianController {

    private final VeterinarianService veterinarianService;

    @Override
    public ResponseEntity<ResponseGeneric> create(VeterinarianCreateDTO dto) {
        log.debug("START create veterinarian");
        try {
            return ok(veterinarianService.create(dto));
        } catch (UserAlreadyExistsException e) {
            return error(CodesResponse.USER_EXISTS, e);
        } catch (Exception e) {
            return error(CodesResponse.ERROR_SAVE_DATA, e);
        }
    }

    @Override
    public ResponseEntity<ResponseGeneric> list(boolean includeInactive) {
        log.debug("START list veterinarians");
        return handle(CodesResponse.ERROR_GET_DATA, () -> veterinarianService.list(includeInactive));
    }

    @Override
    public ResponseEntity<ResponseGeneric> get(int medicoId) {
        log.debug("START get veterinarian {}", medicoId);
        return handle(CodesResponse.ERROR_GET_DATA, () -> veterinarianService.get(medicoId));
    }

    @Override
    public ResponseEntity<ResponseGeneric> update(int medicoId, VeterinarianUpdateDTO dto) {
        log.debug("START update veterinarian {}", medicoId);
        return handle(CodesResponse.ERROR_MODIFY_DATA, () -> veterinarianService.update(medicoId, dto));
    }

    @Override
    public ResponseEntity<ResponseGeneric> changeStatus(int medicoId, VeterinarianStatusDTO dto) {
        log.debug("START changeStatus veterinarian {}", medicoId);
        return handle(CodesResponse.ERROR_MODIFY_DATA, () -> veterinarianService.changeStatus(medicoId, dto.getActivo()));
    }

    private ResponseEntity<ResponseGeneric> handle(CodesResponse errorCode, Supplier<Object> action) {
        try {
            return ok(action.get());
        } catch (Exception e) {
            return error(errorCode, e);
        }
    }

    private ResponseEntity<ResponseGeneric> ok(Object data) {
        ResponseGeneric response = new ResponseGeneric();
        response.setResponseCode(CodesResponse.OK.getCode());
        response.setResponseDesc(CodesResponse.OK.getDescription());
        response.setResponseObj(data);
        return ResponseEntity.ok(response);
    }

    // Igual que el resto de la API: los errores se responden con HTTP 200 y el código de error
    private ResponseEntity<ResponseGeneric> error(CodesResponse code, Exception e) {
        ResponseGeneric response = new ResponseGeneric();
        response.setResponseCode(code.getCode());
        response.setResponseDesc(code.getDescription());
        response.setResponseObj(ErrorMessages.clientMessage(e));
        return ResponseEntity.ok(response);
    }
}
