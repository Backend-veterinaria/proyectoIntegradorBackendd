package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.ClientInfo;
import com.example.proyectoIntegrador.models.ClientUpdateDTO;
import com.example.proyectoIntegrador.repository.ClientRepository;
import com.example.proyectoIntegrador.security.AuthenticatedUser;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.services.Impl.ClientServiceImpl;
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
class ClientServiceImplTest {

    private static final AuthenticatedUser CLIENTE = new AuthenticatedUser(5, "ana", Role.CLIENTE);
    private static final AuthenticatedUser VETERINARIO = new AuthenticatedUser(40, "dr.perez", Role.VETERINARIO);
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1, "admin", Role.ADMIN);

    @Mock
    private ClientRepository clientRepository;

    private ClientServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClientServiceImpl(clientRepository);
    }

    private static ClientInfo client(int id, Role role, boolean activo) {
        ClientInfo info = new ClientInfo();
        info.setClienteId(id);
        info.setTelefono("3001234567");
        info.setRole(role.name());
        info.setActivo(activo);
        return info;
    }

    private static ClientUpdateDTO update(String telefono) {
        ClientUpdateDTO dto = new ClientUpdateDTO();
        dto.setNombre("Ana");
        dto.setApellido("Gomez");
        dto.setDireccion("Calle 1");
        dto.setTelefono(telefono);
        dto.setEmail("ana@correo.com");
        return dto;
    }

    @Test
    void clienteConsultaSuPerfil() {
        when(clientRepository.findById(5)).thenReturn(Optional.of(client(5, Role.CLIENTE, true)));

        assertEquals(5, service.get(CLIENTE, 5).getClienteId());
    }

    @Test
    void clienteNoPuedeConsultarOtroPerfil() {
        assertThrows(AccessDeniedException.class, () -> service.get(CLIENTE, 6));
        verify(clientRepository, never()).findById(anyInt());
    }

    @Test
    void personalConsultaCualquierPerfil() {
        when(clientRepository.findById(6)).thenReturn(Optional.of(client(6, Role.CLIENTE, true)));

        assertEquals(6, service.get(VETERINARIO, 6).getClienteId());
    }

    @Test
    void clienteInexistenteEsErrorDeNegocio() {
        when(clientRepository.findById(6)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> service.get(ADMIN, 6));
        assertEquals("El cliente no existe", e.getMessage());
    }

    @Test
    void clienteEditaSuTelefono() {
        ClientInfo updated = client(5, Role.CLIENTE, true);
        updated.setTelefono("3109999999");
        when(clientRepository.findById(5)).thenReturn(Optional.of(client(5, Role.CLIENTE, true)), Optional.of(updated));
        when(clientRepository.existsEmailForOtherUser("ana@correo.com", 5)).thenReturn(false);
        when(clientRepository.update(5, "Ana", "Gomez", "Calle 1", "3109999999", "ana@correo.com")).thenReturn(1);

        ClientInfo result = service.update(CLIENTE, 5, update(" 3109999999 "));

        assertEquals("3109999999", result.getTelefono());
    }

    @Test
    void clienteNoPuedeEditarOtroPerfil() {
        assertThrows(AccessDeniedException.class, () -> service.update(CLIENTE, 6, update("300")));
        verify(clientRepository, never()).update(anyInt(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void editarConEmailDeOtroUsuarioFalla() {
        when(clientRepository.findById(5)).thenReturn(Optional.of(client(5, Role.CLIENTE, true)));
        when(clientRepository.existsEmailForOtherUser("ana@correo.com", 5)).thenReturn(true);

        BusinessException e = assertThrows(BusinessException.class, () -> service.update(CLIENTE, 5, update("300")));
        assertEquals("El email ya esta registrado por otro usuario", e.getMessage());
    }

    @Test
    void adminInactivaPropietario() {
        when(clientRepository.findById(6))
                .thenReturn(Optional.of(client(6, Role.CLIENTE, true)), Optional.of(client(6, Role.CLIENTE, false)));
        when(clientRepository.updateStatus(6, false)).thenReturn(1);

        ClientInfo result = service.changeStatus(ADMIN, 6, false);

        assertFalse(result.getActivo());
    }

    @Test
    void noSePuedeInactivarLaPropiaCuenta() {
        BusinessException e = assertThrows(BusinessException.class, () -> service.changeStatus(ADMIN, 1, false));
        assertEquals("No puede cambiar el estado de su propia cuenta", e.getMessage());
        verify(clientRepository, never()).updateStatus(anyInt(), anyBoolean());
    }

    @Test
    void elPersonalNoSeInactivaDesdePropietarios() {
        when(clientRepository.findById(40)).thenReturn(Optional.of(client(40, Role.VETERINARIO, true)));

        assertThrows(BusinessException.class, () -> service.changeStatus(ADMIN, 40, false));
        verify(clientRepository, never()).updateStatus(anyInt(), anyBoolean());
    }

    @Test
    void listarPasaFiltros() {
        when(clientRepository.findOwners(true, "gomez")).thenReturn(List.of(client(5, Role.CLIENTE, true)));

        assertEquals(1, service.list(true, "gomez").size());
    }
}
