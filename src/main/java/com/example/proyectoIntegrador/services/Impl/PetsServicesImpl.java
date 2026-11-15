package com.example.proyectoIntegrador.services.Impl;

import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.DataMascotaDTO;
import com.example.proyectoIntegrador.models.DataUserDTO;
import com.example.proyectoIntegrador.models.InfoMascota;
import com.example.proyectoIntegrador.models.PetUpdateDTO;
import com.example.proyectoIntegrador.repository.AgendaCitaRepository;
import com.example.proyectoIntegrador.security.AuthenticatedUser;
import com.example.proyectoIntegrador.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import com.example.proyectoIntegrador.repository.PetsRepository;
import com.example.proyectoIntegrador.services.PetsServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PetsServicesImpl implements PetsServices {

    private final PetsRepository petsRepository;
    private final AgendaCitaRepository agendaCitaRepository;

    /**
     * @param petId
     * @return
     */
    @Override
    public boolean getPetExists(String petId) {
        return false;
    }

    /**
     * @param dataMascotaDTO
     * @return
     */
    @Override
    public boolean addPet(DataMascotaDTO dataMascotaDTO) {

        agendaCitaRepository.validateClient(dataMascotaDTO.getIdCliente())
                .orElseThrow(() -> new BusinessException("cliente not found"));

        var existPet = petsRepository.validateExistPet(dataMascotaDTO.getNombre(), dataMascotaDTO.getEdad(), dataMascotaDTO.getGenero(), dataMascotaDTO.getIdCliente())
                .orElseThrow(() -> new BusinessException("Error. No se pudo validar la existencia de la mascota con los datos ingresados"));

        if (existPet) {
            log.debug("Existe la mascota con los datos ingresados");
            throw new BusinessException("La mascota ya existe");
        } else {
            log.debug("No existe la mascota con los datos ingresados");
        }

        String codigo = UUID.randomUUID().toString().substring(0, 8);

        petsRepository.addPet(dataMascotaDTO.getIdCliente(), codigo, dataMascotaDTO.getNombre(),
                        dataMascotaDTO.getEdad(), dataMascotaDTO.getGenero(), dataMascotaDTO.getRazaId())
                .orElseThrow(() -> new BusinessException("No se pudo agregar la mascota"));

        return true;
    }

    /**
     * @param dataUserDTO
     * @return
     */
    @Override
    public List<InfoMascota> getListPets(DataUserDTO dataUserDTO, boolean includeInactive) {
        agendaCitaRepository.validateClient(dataUserDTO.getIdClient())
                .orElseThrow(() -> new BusinessException("cliente not found"));

        return petsRepository.getListPetsByUser(dataUserDTO.getIdClient(), includeInactive)
                .orElseThrow(() -> new BusinessException("El usuario no tiene mascotas registradas"));
    }
    @Override
    public InfoMascota updatePet(AuthenticatedUser user, int mascotaId, PetUpdateDTO dto) {
        InfoMascota pet = findAccessiblePet(user, mascotaId);

        if (petsRepository.existsOtherPet(dto.getNombre(), dto.getEdad(), dto.getGenero(), pet.getClienteId(), mascotaId)) {
            throw new BusinessException("El cliente ya tiene otra mascota con esos datos");
        }

        if (petsRepository.updatePet(mascotaId, dto.getNombre(), dto.getEdad(), dto.getGenero(), dto.getRazaId()) == 0) {
            throw new BusinessException("La mascota no existe");
        }

        return petsRepository.findById(mascotaId)
                .orElseThrow(() -> new RuntimeException("No se encontro la mascota recien actualizada"));
    }

    @Override
    public InfoMascota changeStatus(AuthenticatedUser user, int mascotaId, boolean activo) {
        findAccessiblePet(user, mascotaId);

        if (petsRepository.updateStatus(mascotaId, activo) == 0) {
            throw new BusinessException("La mascota no existe");
        }

        log.info("Mascota {} {} por el usuario {}", mascotaId, activo ? "activada" : "inactivada", user.userId());
        return petsRepository.findById(mascotaId)
                .orElseThrow(() -> new RuntimeException("No se encontro la mascota recien actualizada"));
    }

    /**
     * La mascota debe existir y el usuario debe poder acceder a su dueño.
     */
    private InfoMascota findAccessiblePet(AuthenticatedUser user, int mascotaId) {
        InfoMascota pet = petsRepository.findById(mascotaId)
                .orElseThrow(() -> new BusinessException("La mascota no existe"));

        if (!SecurityUtils.canAccessClient(user, pet.getClienteId())) {
            throw new AccessDeniedException("Solo puede gestionar sus propias mascotas");
        }
        return pet;
    }
}
