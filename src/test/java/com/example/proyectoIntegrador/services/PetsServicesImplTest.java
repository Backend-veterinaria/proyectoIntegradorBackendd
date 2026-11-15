package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.DataUserDTO;
import com.example.proyectoIntegrador.models.InfoMascota;
import com.example.proyectoIntegrador.models.PetUpdateDTO;
import com.example.proyectoIntegrador.repository.AgendaCitaRepository;
import com.example.proyectoIntegrador.repository.PetsRepository;
import com.example.proyectoIntegrador.security.AuthenticatedUser;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.services.Impl.PetsServicesImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetsServicesImplTest {

    private static final AuthenticatedUser CLIENTE = new AuthenticatedUser(5, "ana", Role.CLIENTE);
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1, "admin", Role.ADMIN);

    @Mock
    private PetsRepository petsRepository;
    @Mock
    private AgendaCitaRepository agendaCitaRepository;

    private PetsServicesImpl service;

    @BeforeEach
    void setUp() {
        service = new PetsServicesImpl(petsRepository, agendaCitaRepository);
    }

    private static InfoMascota pet(int clienteId, boolean activo) {
        InfoMascota pet = new InfoMascota();
        pet.setIdMascota(8);
        pet.setClienteId(clienteId);
        pet.setNombreMascota("Luna");
        pet.setActivo(activo);
        return pet;
    }

    private static PetUpdateDTO update() {
        PetUpdateDTO dto = new PetUpdateDTO();
        dto.setNombre("Luna");
        dto.setEdad(4);
        dto.setGenero("Hembra");
        dto.setRazaId(2);
        return dto;
    }

    @Test
    void clienteEditaSuMascota() {
        when(petsRepository.findById(8)).thenReturn(Optional.of(pet(5, true)));
        when(petsRepository.existsOtherPet("Luna", 4, "Hembra", 5, 8)).thenReturn(false);
        when(petsRepository.updatePet(8, "Luna", 4, "Hembra", 2)).thenReturn(1);

        service.updatePet(CLIENTE, 8, update());

        verify(petsRepository).updatePet(8, "Luna", 4, "Hembra", 2);
    }

    @Test
    void clienteNoPuedeEditarMascotaDeOtro() {
        when(petsRepository.findById(8)).thenReturn(Optional.of(pet(6, true)));

        assertThrows(AccessDeniedException.class, () -> service.updatePet(CLIENTE, 8, update()));
        verify(petsRepository, never()).updatePet(anyInt(), anyString(), anyInt(), anyString(), anyInt());
    }

    @Test
    void personalPuedeEditarCualquierMascota() {
        when(petsRepository.findById(8)).thenReturn(Optional.of(pet(6, true)));
        when(petsRepository.existsOtherPet("Luna", 4, "Hembra", 6, 8)).thenReturn(false);
        when(petsRepository.updatePet(8, "Luna", 4, "Hembra", 2)).thenReturn(1);

        service.updatePet(ADMIN, 8, update());

        verify(petsRepository).updatePet(8, "Luna", 4, "Hembra", 2);
    }

    @Test
    void editarConDatosDeOtraMascotaDelClienteFalla() {
        when(petsRepository.findById(8)).thenReturn(Optional.of(pet(5, true)));
        when(petsRepository.existsOtherPet("Luna", 4, "Hembra", 5, 8)).thenReturn(true);

        BusinessException e = assertThrows(BusinessException.class, () -> service.updatePet(CLIENTE, 8, update()));
        assertEquals("El cliente ya tiene otra mascota con esos datos", e.getMessage());
    }

    @Test
    void editarMascotaInexistenteFalla() {
        when(petsRepository.findById(8)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> service.updatePet(CLIENTE, 8, update()));
        assertEquals("La mascota no existe", e.getMessage());
    }

    @Test
    void inactivarMascota() {
        when(petsRepository.findById(8)).thenReturn(Optional.of(pet(5, true)), Optional.of(pet(5, false)));
        when(petsRepository.updateStatus(8, false)).thenReturn(1);

        InfoMascota result = service.changeStatus(CLIENTE, 8, false);

        assertFalse(result.getActivo());
    }

    @Test
    void clienteNoPuedeInactivarMascotaDeOtro() {
        when(petsRepository.findById(8)).thenReturn(Optional.of(pet(6, true)));

        assertThrows(AccessDeniedException.class, () -> service.changeStatus(CLIENTE, 8, false));
        verify(petsRepository, never()).updateStatus(anyInt(), anyBoolean());
    }

    @Test
    void listarPorDefectoSoloActivas() {
        DataUserDTO dto = new DataUserDTO();
        dto.setIdClient(5);
        when(agendaCitaRepository.validateClient(5)).thenReturn(Optional.of(true));
        when(petsRepository.getListPetsByUser(5, false)).thenReturn(Optional.of(List.of(pet(5, true))));

        assertEquals(1, service.getListPets(dto, false).size());
        verify(petsRepository).getListPetsByUser(5, false);
    }
}
