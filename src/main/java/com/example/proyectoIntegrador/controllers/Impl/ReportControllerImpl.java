package com.example.proyectoIntegrador.controllers.Impl;

import com.example.proyectoIntegrador.controllers.ReportController;
import com.example.proyectoIntegrador.enums.CodesResponse;
import com.example.proyectoIntegrador.exceptions.ErrorMessages;
import com.example.proyectoIntegrador.models.ReportFilterDTO;
import com.example.proyectoIntegrador.models.ResponseGeneric;
import com.example.proyectoIntegrador.security.SecurityUtils;
import com.example.proyectoIntegrador.services.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ReportControllerImpl implements ReportController {

    private final ReportService reportService;

    @Override
    public ResponseEntity<ResponseGeneric> appointmentsByStatus(ReportFilterDTO filter) {
        log.debug("START report appointmentsByStatus");
        return handle(() -> reportService.appointmentsByStatus(SecurityUtils.currentUser(), filter));
    }

    @Override
    public ResponseEntity<ResponseGeneric> attentionsByVeterinarian(ReportFilterDTO filter) {
        log.debug("START report attentionsByVeterinarian");
        return handle(() -> reportService.attentionsByVeterinarian(SecurityUtils.currentUser(), filter));
    }

    @Override
    public ResponseEntity<ResponseGeneric> attendedPets(ReportFilterDTO filter) {
        log.debug("START report attendedPets");
        return handle(() -> reportService.attendedPets(SecurityUtils.currentUser(), filter));
    }

    private ResponseEntity<ResponseGeneric> handle(Supplier<Object> action) {
        ResponseGeneric response = new ResponseGeneric();
        try {
            response.setResponseObj(action.get());
            response.setResponseCode(CodesResponse.OK.getCode());
            response.setResponseDesc(CodesResponse.OK.getDescription());
        } catch (AccessDeniedException e) {
            // Se deja pasar para que GlobalExceptionHandler responda 403
            throw e;
        } catch (Exception e) {
            response.setResponseCode(CodesResponse.ERROR_GET_DATA.getCode());
            response.setResponseDesc(CodesResponse.ERROR_GET_DATA.getDescription());
            response.setResponseObj(ErrorMessages.clientMessage(e));
        }
        return ResponseEntity.ok().body(response);
    }
}
