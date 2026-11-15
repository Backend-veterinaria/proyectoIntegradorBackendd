package com.example.proyectoIntegrador.controllers.Impl;

import com.example.proyectoIntegrador.exceptions.ErrorMessages;
import com.example.proyectoIntegrador.controllers.AgendaController;
import com.example.proyectoIntegrador.enums.CodesResponse;
import com.example.proyectoIntegrador.models.*;
import com.example.proyectoIntegrador.services.AgendaService;
import com.example.proyectoIntegrador.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@Slf4j
public class AgendaControllerImpl implements AgendaController {

    private final AgendaService agendaService;

    @Override
    public ResponseEntity<ResponseGeneric> getListAgend(DataUserDTO dataUser, HttpServletRequest request) {
        log.debug("START getListAgend");
        SecurityUtils.requireSameClient(dataUser.getIdClient());

        try {
            List<AgendaCita> userInfoLogin = agendaService.getListCitas(dataUser.getIdClient());

            ResponseGeneric responseLogin = new ResponseGeneric();
            responseLogin.setResponseCode(CodesResponse.OK.getCode());
            responseLogin.setResponseDesc(CodesResponse.OK.getDescription());
            responseLogin.setResponseObj(userInfoLogin);

            return ResponseEntity.ok().body(responseLogin);

        } catch (Exception e) {
            ResponseLogin responseError = new ResponseLogin();
            responseError.setResponseCode(CodesResponse.ERROR_GET_DATA.getCode());
            responseError.setResponseDesc(CodesResponse.ERROR_GET_DATA.getDescription());
            responseError.setResponseObj(ErrorMessages.clientMessage(e));

            return ResponseEntity.status(HttpStatus.OK).body(responseError);
        }
    }

    @Override
    public ResponseEntity<ResponseGeneric> scheduleAnAppointment(DataAppointmentDTO dataAppointmentDTO, HttpServletRequest request) {
        log.debug("START scheduleAnAppointment");
        SecurityUtils.requireSameClient(dataAppointmentDTO.getClienteId());

        try {
            Boolean userInfoLogin = agendaService.scheduleAnAppointment(dataAppointmentDTO);

            ResponseGeneric responseLogin = new ResponseGeneric();
            responseLogin.setResponseCode(CodesResponse.OK.getCode());
            responseLogin.setResponseDesc(CodesResponse.OK.getDescription());
            responseLogin.setResponseObj(userInfoLogin);

            return ResponseEntity.ok().body(responseLogin);

        } catch (Exception e) {
            ResponseLogin responseError = new ResponseLogin();
            responseError.setResponseCode(CodesResponse.ERROR_SAVE_AGEND.getCode());
            responseError.setResponseDesc(CodesResponse.ERROR_SAVE_AGEND.getDescription());
            responseError.setResponseObj(ErrorMessages.clientMessage(e));

            return ResponseEntity.status(HttpStatus.OK).body(responseError);
        }
    }

    /**
     * @param dataAppointmentDTO
     * @param request
     * @return
     */
    @Override
    public ResponseEntity<ResponseGeneric> cancelledAppointment(DataCancelledAppointmentDTO dataAppointmentDTO, HttpServletRequest request) {
        log.debug("START cancelledAppointment");
        SecurityUtils.requireSameClient(dataAppointmentDTO.getIdCliente());

        try {
            var infoAppointment = agendaService.cancelledAppointment(dataAppointmentDTO);

            ResponseGeneric responseLogin = new ResponseGeneric();
            responseLogin.setResponseCode(CodesResponse.OK.getCode());
            responseLogin.setResponseDesc(CodesResponse.OK.getDescription());
            responseLogin.setResponseObj(infoAppointment);

            return ResponseEntity.ok().body(responseLogin);

        } catch (Exception e) {
            ResponseLogin responseError = new ResponseLogin();
            responseError.setResponseCode(CodesResponse.ERROR_MODIFY_DATA.getCode());
            responseError.setResponseDesc(CodesResponse.ERROR_MODIFY_DATA.getDescription());
            responseError.setResponseObj(ErrorMessages.clientMessage(e));

            return ResponseEntity.status(HttpStatus.OK).body(responseError);
        }
    }

    @Override
    public ResponseEntity<ResponseGeneric> getVeterinarianAgenda(DataVeterinarianAgendaDTO dto) {
        log.debug("START getVeterinarianAgenda");

        try {
            var agenda = agendaService.getVeterinarianAgenda(SecurityUtils.currentUser(), dto);

            ResponseGeneric response = new ResponseGeneric();
            response.setResponseCode(CodesResponse.OK.getCode());
            response.setResponseDesc(CodesResponse.OK.getDescription());
            response.setResponseObj(agenda);
            return ResponseEntity.ok().body(response);

        } catch (AccessDeniedException e) {
            // Se deja pasar para que GlobalExceptionHandler responda 403
            throw e;
        } catch (Exception e) {
            ResponseGeneric responseError = new ResponseGeneric();
            responseError.setResponseCode(CodesResponse.ERROR_GET_DATA.getCode());
            responseError.setResponseDesc(CodesResponse.ERROR_GET_DATA.getDescription());
            responseError.setResponseObj(ErrorMessages.clientMessage(e));
            return ResponseEntity.status(HttpStatus.OK).body(responseError);
        }
    }

    @Override
    public ResponseEntity<ResponseGeneric> changeAppointmentStatus(int agendaId, AppointmentStatusDTO dto) {
        log.debug("START changeAppointmentStatus");

        try {
            var cita = agendaService.changeAppointmentStatus(SecurityUtils.currentUser(), agendaId, dto.getEstadoId());

            ResponseGeneric response = new ResponseGeneric();
            response.setResponseCode(CodesResponse.OK.getCode());
            response.setResponseDesc(CodesResponse.OK.getDescription());
            response.setResponseObj(cita);
            return ResponseEntity.ok().body(response);

        } catch (AccessDeniedException e) {
            // Se deja pasar para que GlobalExceptionHandler responda 403
            throw e;
        } catch (Exception e) {
            ResponseGeneric responseError = new ResponseGeneric();
            responseError.setResponseCode(CodesResponse.ERROR_MODIFY_DATA.getCode());
            responseError.setResponseDesc(CodesResponse.ERROR_MODIFY_DATA.getDescription());
            responseError.setResponseObj(ErrorMessages.clientMessage(e));
            return ResponseEntity.status(HttpStatus.OK).body(responseError);
        }
    }

    @Override
    public ResponseEntity<ResponseGeneric> rescheduleAppointment(int agendaId, RescheduleAppointmentDTO dto) {
        log.debug("START rescheduleAppointment");

        try {
            var cita = agendaService.rescheduleAppointment(SecurityUtils.currentUser(), agendaId, dto);

            ResponseGeneric response = new ResponseGeneric();
            response.setResponseCode(CodesResponse.OK.getCode());
            response.setResponseDesc(CodesResponse.OK.getDescription());
            response.setResponseObj(cita);
            return ResponseEntity.ok().body(response);

        } catch (AccessDeniedException e) {
            // Se deja pasar para que GlobalExceptionHandler responda 403
            throw e;
        } catch (Exception e) {
            ResponseGeneric responseError = new ResponseGeneric();
            responseError.setResponseCode(CodesResponse.ERROR_MODIFY_DATA.getCode());
            responseError.setResponseDesc(CodesResponse.ERROR_MODIFY_DATA.getDescription());
            responseError.setResponseObj(ErrorMessages.clientMessage(e));
            return ResponseEntity.status(HttpStatus.OK).body(responseError);
        }
    }

    @Override
    public ResponseEntity<ResponseGeneric> getDetailAppointment(DataDetailAppointmentDTO detailAppointmentDTO, HttpServletRequest request) {

        log.debug("START getDetailAppointment");
        SecurityUtils.requireSameClient(detailAppointmentDTO.getIdClient());
        SecurityUtils.requireSameUsername(detailAppointmentDTO.getUsername());

        try {
            AgendaCitaDetail infoAppointment = agendaService.getDetailAppointment(detailAppointmentDTO);

            ResponseGeneric responseLogin = new ResponseGeneric();
            responseLogin.setResponseCode(CodesResponse.OK.getCode());
            responseLogin.setResponseDesc(CodesResponse.OK.getDescription());
            responseLogin.setResponseObj(infoAppointment);

            return ResponseEntity.ok().body(responseLogin);

        } catch (Exception e) {
            ResponseLogin responseError = new ResponseLogin();
            responseError.setResponseCode(CodesResponse.ERROR_GET_DATA.getCode());
            responseError.setResponseDesc(CodesResponse.ERROR_GET_DATA.getDescription());
            responseError.setResponseObj(ErrorMessages.clientMessage(e));

            return ResponseEntity.status(HttpStatus.OK).body(responseError);
        }
    }
}
