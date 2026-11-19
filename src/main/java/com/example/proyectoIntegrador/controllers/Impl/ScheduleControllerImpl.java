package com.example.proyectoIntegrador.controllers.Impl;

import com.example.proyectoIntegrador.controllers.ScheduleController;
import com.example.proyectoIntegrador.enums.CodesResponse;
import com.example.proyectoIntegrador.exceptions.ErrorMessages;
import com.example.proyectoIntegrador.models.ResponseGeneric;
import com.example.proyectoIntegrador.models.ScheduleModels.ParameterValueDTO;
import com.example.proyectoIntegrador.models.ScheduleModels.WeekScheduleDTO;
import com.example.proyectoIntegrador.services.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ScheduleControllerImpl implements ScheduleController {

    private final ScheduleService scheduleService;

    @Override
    public ResponseEntity<ResponseGeneric> getWeek(Integer medicoId) {
        return handle(CodesResponse.ERROR_GET_DATA, () -> scheduleService.getWeek(medicoId));
    }

    @Override
    public ResponseEntity<ResponseGeneric> replaceWeek(Integer medicoId, WeekScheduleDTO dto) {
        return handle(CodesResponse.ERROR_MODIFY_DATA, () -> scheduleService.replaceWeek(medicoId, dto));
    }

    @Override
    public ResponseEntity<ResponseGeneric> parameters() {
        return handle(CodesResponse.ERROR_GET_DATA, scheduleService::parameters);
    }

    @Override
    public ResponseEntity<ResponseGeneric> updateParameter(String clave, ParameterValueDTO dto) {
        return handle(CodesResponse.ERROR_MODIFY_DATA, () -> scheduleService.updateParameter(clave, dto.getValor()));
    }

    private ResponseEntity<ResponseGeneric> handle(CodesResponse errorCode, Supplier<Object> action) {
        ResponseGeneric response = new ResponseGeneric();
        try {
            response.setResponseObj(action.get());
            response.setResponseCode(CodesResponse.OK.getCode());
            response.setResponseDesc(CodesResponse.OK.getDescription());
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            response.setResponseCode(errorCode.getCode());
            response.setResponseDesc(errorCode.getDescription());
            response.setResponseObj(ErrorMessages.clientMessage(e));
        }
        return ResponseEntity.ok(response);
    }
}
