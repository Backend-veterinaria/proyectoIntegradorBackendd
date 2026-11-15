package com.example.proyectoIntegrador.controllers.Impl;

import com.example.proyectoIntegrador.exceptions.ErrorMessages;
import com.example.proyectoIntegrador.controllers.PetsController;
import com.example.proyectoIntegrador.enums.CodesResponse;
import com.example.proyectoIntegrador.models.DataMascotaDTO;
import com.example.proyectoIntegrador.models.DataUserDTO;
import com.example.proyectoIntegrador.models.PetUpdateDTO;
import com.example.proyectoIntegrador.models.StatusDTO;
import org.springframework.security.access.AccessDeniedException;
import java.util.function.Supplier;
import com.example.proyectoIntegrador.models.ResponseGeneric;
import com.example.proyectoIntegrador.models.ResponseLogin;
import com.example.proyectoIntegrador.services.PetsServices;
import com.example.proyectoIntegrador.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Slf4j
public class PetsControllerImpl implements PetsController {

    private final PetsServices petsServices;

    /**
     * @param dataMascotaDTO
     * @param request
     * @return
     */
    @Override
    public ResponseEntity<ResponseGeneric> addPets(DataMascotaDTO dataMascotaDTO, HttpServletRequest request) {
        log.debug("START addPets");
        SecurityUtils.requireSameClient(dataMascotaDTO.getIdCliente());

        try {
            var userInfoLogin = petsServices.addPet(dataMascotaDTO);

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

    /**
     * @param dataUserDTO
     * @param request
     * @return
     */
    @Override
    public ResponseEntity<ResponseGeneric> getListPets(DataUserDTO dataUserDTO, boolean includeInactive, HttpServletRequest request) {
        log.debug("START getListPets");
        SecurityUtils.requireSameClient(dataUserDTO.getIdClient());

        try {
            var userInfoLogin = petsServices.getListPets(dataUserDTO, includeInactive);

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
    public ResponseEntity<ResponseGeneric> updatePet(int mascotaId, PetUpdateDTO dto) {
        log.debug("START updatePet {}", mascotaId);
        return handle(() -> petsServices.updatePet(SecurityUtils.currentUser(), mascotaId, dto));
    }

    @Override
    public ResponseEntity<ResponseGeneric> changeStatus(int mascotaId, StatusDTO dto) {
        log.debug("START changeStatus pet {}", mascotaId);
        return handle(() -> petsServices.changeStatus(SecurityUtils.currentUser(), mascotaId, dto.getActivo()));
    }

    private ResponseEntity<ResponseGeneric> handle(Supplier<Object> action) {
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
            responseError.setResponseCode(CodesResponse.ERROR_MODIFY_DATA.getCode());
            responseError.setResponseDesc(CodesResponse.ERROR_MODIFY_DATA.getDescription());
            responseError.setResponseObj(ErrorMessages.clientMessage(e));
            return ResponseEntity.ok(responseError);
        }
    }
}
