package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.models.DataMascotaDTO;
import com.example.proyectoIntegrador.models.DataUserDTO;
import com.example.proyectoIntegrador.models.InfoMascota;
import com.example.proyectoIntegrador.models.PetUpdateDTO;
import com.example.proyectoIntegrador.security.AuthenticatedUser;

import java.util.List;

public interface PetsServices {

    boolean getPetExists(String petId);

    boolean addPet(DataMascotaDTO dataMascotaDTO);

    /**
     * @param includeInactive si es false, solo las mascotas activas (las que se ofrecen para nuevas citas)
     */
    List<InfoMascota> getListPets(DataUserDTO dataUserDTO, boolean includeInactive);

    /**
     * Edita una mascota (CIT-02). El cliente solo sus mascotas; el personal, cualquiera.
     *
     * @throws org.springframework.security.access.AccessDeniedException si la mascota no es del cliente
     */
    InfoMascota updatePet(AuthenticatedUser user, int mascotaId, PetUpdateDTO dto);

    /**
     * Activa o inactiva una mascota (CIT-02). Una mascota inactiva conserva su historial, pero no se
     * ofrece ni se le pueden agendar nuevas citas.
     */
    InfoMascota changeStatus(AuthenticatedUser user, int mascotaId, boolean activo);
}
