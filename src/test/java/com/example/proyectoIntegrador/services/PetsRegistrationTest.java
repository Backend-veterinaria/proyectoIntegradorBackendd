package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.DataMascotaDTO;
import com.example.proyectoIntegrador.models.DataUserDTO;
import com.example.proyectoIntegrador.models.InfoMascota;
import com.example.proyectoIntegrador.repository.AgendaCitaRepository;
import com.example.proyectoIntegrador.repository.PetsRepository;
import com.example.proyectoIntegrador.services.Impl.PetsServicesImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Registro y consulta de mascotas (funcionalidad base).
 */
@ExtendWith(MockitoExtension.class)
class PetsRegistrationTest {

    @Mock
    private PetsRepository petsRepository;
    @Mock
    private AgendaCitaRepository agendaCitaRepository;

    private PetsServicesImpl service;

    @BeforeEach
    void setUp() {
        service = new PetsServicesImpl(petsRepository, agendaCitaRepository);
    }

    private static DataMascotaDTO nueva() {
        return new DataMascotaDTO(5, "Firulais", 3, "Macho", 2);
    }

    private static DataUserDTO user(int id) {
        DataUserDTO dto = new DataUserDTO();
        dto.setIdClient(id);
        return dto;
    }

    @Test
    void registraMascotaConCodigoDeOchoCaracteres() {
        when(agendaCitaRepository.validateClient(5)).thenReturn(Optional.of(true));
        when(petsRepository.validateExistPet("Firulais", 3, "Macho", 5)).thenReturn(Optional.of(false));
        when(petsRepository.addPet(eq(5), anyString(), eq("Firulais"), eq(3), eq("Macho"), eq(2))).thenReturn(Optional.of(true));

        assertTrue(service.addPet(nueva()));

        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(petsRepository).addPet(eq(5), codigo.capture(), any(), any(), any(), any());
        assertEquals(8, codigo.getValue().length());
    }

    @Test
    void mascotaDuplicadaNoSeRegistra() {
        when(agendaCitaRepository.validateClient(5)).thenReturn(Optional.of(true));
        when(petsRepository.validateExistPet("Firulais", 3, "Macho", 5)).thenReturn(Optional.of(true));

        BusinessException e = assertThrows(BusinessException.class, () -> service.addPet(nueva()));
        assertEquals("La mascota ya existe", e.getMessage());
        verify(petsRepository, never()).addPet(anyInt(), anyString(), anyString(), anyInt(), anyString(), anyInt());
    }

    @Test
    void clienteInexistenteNoRegistraMascota() {
        when(agendaCitaRepository.validateClient(5)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.addPet(nueva()));
        verify(petsRepository, never()).validateExistPet(anyString(), anyInt(), anyString(), anyInt());
    }

    @Test
    void falloAlValidarDuplicadoEsErrorDeNegocio() {
        when(agendaCitaRepository.validateClient(5)).thenReturn(Optional.of(true));
        when(petsRepository.validateExistPet("Firulais", 3, "Macho", 5)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.addPet(nueva()));
    }

    @Test
    void falloAlInsertarEsErrorDeNegocio() {
        when(agendaCitaRepository.validateClient(5)).thenReturn(Optional.of(true));
        when(petsRepository.validateExistPet("Firulais", 3, "Macho", 5)).thenReturn(Optional.of(false));
        when(petsRepository.addPet(eq(5), anyString(), any(), any(), any(), any())).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> service.addPet(nueva()));
        assertEquals("No se pudo agregar la mascota", e.getMessage());
    }

    @Test
    void listaLasMascotasDelCliente() {
        when(agendaCitaRepository.validateClient(5)).thenReturn(Optional.of(true));
        when(petsRepository.getListPetsByUser(5, false)).thenReturn(Optional.of(List.of(new InfoMascota())));

        assertEquals(1, service.getListPets(user(5), false).size());
    }

    @Test
    void clienteSinMascotasEsErrorDeNegocio() {
        when(agendaCitaRepository.validateClient(5)).thenReturn(Optional.of(true));
        when(petsRepository.getListPetsByUser(5, true)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> service.getListPets(user(5), true));
        assertEquals("El usuario no tiene mascotas registradas", e.getMessage());
    }
}
