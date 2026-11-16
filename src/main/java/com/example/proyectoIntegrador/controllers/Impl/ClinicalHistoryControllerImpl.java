package com.example.proyectoIntegrador.controllers.Impl;

import com.example.proyectoIntegrador.controllers.ClinicalHistoryController;
import com.example.proyectoIntegrador.enums.CodesResponse;
import com.example.proyectoIntegrador.enums.TreatmentStatus;
import com.example.proyectoIntegrador.exceptions.ErrorMessages;
import com.example.proyectoIntegrador.models.ClinicalAttentionCreateDTO;
import com.example.proyectoIntegrador.models.ResponseGeneric;
import com.example.proyectoIntegrador.models.TreatmentCreateDTO;
import com.example.proyectoIntegrador.models.TreatmentStatusDTO;
import com.example.proyectoIntegrador.models.VaccineCreateDTO;
import com.example.proyectoIntegrador.security.SecurityUtils;
import com.example.proyectoIntegrador.services.ClinicalHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ClinicalHistoryControllerImpl implements ClinicalHistoryController {

    private final ClinicalHistoryService clinicalHistoryService;

    @Override
    public ResponseEntity<ResponseGeneric> registerAttention(ClinicalAttentionCreateDTO dto) {
        log.debug("START registerAttention agenda={}", dto.getAgendaId());
        return handle(CodesResponse.ERROR_SAVE_DATA,
                () -> clinicalHistoryService.registerAttention(SecurityUtils.currentUser(), dto));
    }

    @Override
    public ResponseEntity<ResponseGeneric> getAttention(int atencionId) {
        log.debug("START getAttention {}", atencionId);
        return handle(CodesResponse.ERROR_GET_DATA,
                () -> clinicalHistoryService.getAttention(SecurityUtils.currentUser(), atencionId));
    }

    @Override
    public ResponseEntity<ResponseGeneric> getPetHistory(int mascotaId) {
        log.debug("START getPetHistory {}", mascotaId);
        return handle(CodesResponse.ERROR_GET_DATA,
                () -> clinicalHistoryService.getPetHistory(SecurityUtils.currentUser(), mascotaId));
    }

    @Override
    public ResponseEntity<ResponseGeneric> registerTreatment(TreatmentCreateDTO dto) {
        log.debug("START registerTreatment mascota={}", dto.getMascotaId());
        return handle(CodesResponse.ERROR_SAVE_DATA,
                () -> clinicalHistoryService.registerTreatment(SecurityUtils.currentUser(), dto));
    }

    @Override
    public ResponseEntity<ResponseGeneric> changeTreatmentStatus(int tratamientoId, TreatmentStatusDTO dto) {
        log.debug("START changeTreatmentStatus {}", tratamientoId);
        return handle(CodesResponse.ERROR_MODIFY_DATA, () -> clinicalHistoryService.changeTreatmentStatus(
                SecurityUtils.currentUser(), tratamientoId, TreatmentStatus.valueOf(dto.getEstado())));
    }

    @Override
    public ResponseEntity<ResponseGeneric> registerVaccine(VaccineCreateDTO dto) {
        log.debug("START registerVaccine mascota={}", dto.getMascotaId());
        return handle(CodesResponse.ERROR_SAVE_DATA,
                () -> clinicalHistoryService.registerVaccine(SecurityUtils.currentUser(), dto));
    }

    @Override
    public ResponseEntity<ResponseGeneric> getUpcomingVaccines(int days, Integer clienteId) {
        log.debug("START getUpcomingVaccines days={}", days);
        return handle(CodesResponse.ERROR_GET_DATA,
                () -> clinicalHistoryService.getUpcomingVaccines(SecurityUtils.currentUser(), days, clienteId));
    }

    private ResponseEntity<ResponseGeneric> handle(CodesResponse errorCode, Supplier<Object> action) {
        try {
            ResponseGeneric response = new ResponseGeneric();
            response.setResponseCode(CodesResponse.OK.getCode());
            response.setResponseDesc(CodesResponse.OK.getDescription());
            response.setResponseObj(action.get());
            return ResponseEntity.ok().body(response);
        } catch (AccessDeniedException e) {
            // Se deja pasar para que GlobalExceptionHandler responda 403
            throw e;
        } catch (Exception e) {
            ResponseGeneric response = new ResponseGeneric();
            response.setResponseCode(errorCode.getCode());
            response.setResponseDesc(errorCode.getDescription());
            response.setResponseObj(ErrorMessages.clientMessage(e));
            return ResponseEntity.ok().body(response);
        }
    }
}
