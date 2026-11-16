package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.exceptions.UserAlreadyExistsException;
import com.example.proyectoIntegrador.models.ClientCreateDTO;
import com.example.proyectoIntegrador.models.ClientInfo;
import com.example.proyectoIntegrador.models.ClientTemporaryPassword;
import com.example.proyectoIntegrador.models.PasswordChangeDTO;
import com.example.proyectoIntegrador.models.RegisterDTO;
import com.example.proyectoIntegrador.repository.ClientRepository;
import com.example.proyectoIntegrador.repository.ProyectoRepository;
import com.example.proyectoIntegrador.security.AuthenticatedUser;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.services.Impl.ClientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * Registro de propietarios por el personal y contraseñas (CIT-06).
 */
@ExtendWith(MockitoExtension.class)
class ClientAccountServiceTest {

    private static final AuthenticatedUser CLIENTE = new AuthenticatedUser(5, "ana", Role.CLIENTE);
    private static final AuthenticatedUser VETERINARIO = new AuthenticatedUser(40, "dr.perez", Role.VETERINARIO);
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1, "admin", Role.ADMIN);

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ProyectoRepository proyectoRepository;

    private ClientServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClientServiceImpl(clientRepository, proyectoRepository, encoder);
    }

    private static ClientCreateDTO nuevo() {
        ClientCreateDTO dto = new ClientCreateDTO();
        dto.setNombre(" Luis ");
        dto.setApellido("Diaz");
        dto.setDireccion("  ");
        dto.setTelefono("3001234567");
        dto.setEmail("luis@correo.com");
        dto.setUsername("luis.diaz");
        return dto;
    }

    private static ClientInfo owner(int id, Role role) {
        ClientInfo info = new ClientInfo();
        info.setClienteId(id);
        info.setRole(role.name());
        return info;
    }

    private static PasswordChangeDTO change(String actual, String nueva) {
        PasswordChangeDTO dto = new PasswordChangeDTO();
        dto.setPasswordActual(actual);
        dto.setPasswordNueva(nueva);
        return dto;
    }

    // ---------- Registro por el personal ----------

    @Test
    void personalRegistraPropietarioConPasswordTemporal() {
        when(proyectoRepository.existsUsernameOrEmail("luis.diaz", "luis@correo.com")).thenReturn(false);
        when(proyectoRepository.getRoleId(Role.CLIENTE)).thenReturn(Optional.of(3));
        when(proyectoRepository.registerUser(any(), eq(3))).thenReturn(Optional.of(1));
        when(proyectoRepository.getIdUser("luis.diaz")).thenReturn(Optional.of(77));
        when(proyectoRepository.savePasswordUser(eq(77), anyString())).thenReturn(Optional.of(1));
        when(proyectoRepository.setMustChangePassword(77, true)).thenReturn(1);
        when(clientRepository.findById(77)).thenReturn(Optional.of(owner(77, Role.CLIENTE)));

        ClientTemporaryPassword result = service.create(VETERINARIO, nuevo());

        assertEquals(77, result.getCliente().getClienteId());
        assertEquals(10, result.getPasswordTemporal().length());
        // Se guarda el hash, nunca la contraseña temporal en texto plano
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(proyectoRepository).savePasswordUser(eq(77), hash.capture());
        assertTrue(encoder.matches(result.getPasswordTemporal(), hash.getValue()));
        // Datos limpios y sin dirección
        ArgumentCaptor<RegisterDTO> registro = ArgumentCaptor.forClass(RegisterDTO.class);
        verify(proyectoRepository).registerUser(registro.capture(), eq(3));
        assertEquals("Luis", registro.getValue().getNombre());
        assertNull(registro.getValue().getDireccion());
    }

    @Test
    void usuarioOEmailRepetidoNoSeRegistra() {
        when(proyectoRepository.existsUsernameOrEmail("luis.diaz", "luis@correo.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> service.create(ADMIN, nuevo()));
        verify(proyectoRepository, never()).registerUser(any(), anyInt());
    }

    @Test
    void clienteNoRegistraPropietarios() {
        assertThrows(AccessDeniedException.class, () -> service.create(CLIENTE, nuevo()));
        verify(proyectoRepository, never()).existsUsernameOrEmail(anyString(), anyString());
    }

    @Test
    void lasPasswordsTemporalesNoSeRepiten() {
        String a = generate();
        String b = generate();
        assertTrue(a.matches("[A-Za-z2-9]{10}"), a);
        assertTrue(!a.equals(b));
    }

    private String generate() {
        when(proyectoRepository.updatePasswordHash(eq(6), anyString())).thenReturn(Optional.of(1));
        when(clientRepository.findById(6)).thenReturn(Optional.of(owner(6, Role.CLIENTE)));
        return service.resetPassword(ADMIN, 6).getPasswordTemporal();
    }

    // ---------- Restablecer ----------

    @Test
    void personalRestableceLaPasswordDeUnPropietario() {
        when(clientRepository.findById(6)).thenReturn(Optional.of(owner(6, Role.CLIENTE)));
        when(proyectoRepository.updatePasswordHash(eq(6), anyString())).thenReturn(Optional.of(1));

        ClientTemporaryPassword result = service.resetPassword(VETERINARIO, 6);

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(proyectoRepository).updatePasswordHash(eq(6), hash.capture());
        assertTrue(encoder.matches(result.getPasswordTemporal(), hash.getValue()));
        verify(proyectoRepository).setMustChangePassword(6, true);
    }

    @Test
    void noSeRestableceLaPasswordDelPersonal() {
        when(clientRepository.findById(40)).thenReturn(Optional.of(owner(40, Role.VETERINARIO)));

        assertThrows(BusinessException.class, () -> service.resetPassword(ADMIN, 40));
        verify(proyectoRepository, never()).updatePasswordHash(anyInt(), anyString());
    }

    // ---------- Cambio por el propio usuario ----------

    @Test
    void usuarioCambiaSuPassword() {
        when(proyectoRepository.getPasswordHash(5)).thenReturn(Optional.of(encoder.encode("Temporal23")));
        when(proyectoRepository.updatePasswordHash(eq(5), anyString())).thenReturn(Optional.of(1));

        service.changePassword(CLIENTE, 5, change("Temporal23", "MiClaveNueva1"));

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(proyectoRepository).updatePasswordHash(eq(5), hash.capture());
        assertTrue(encoder.matches("MiClaveNueva1", hash.getValue()));
        verify(proyectoRepository).setMustChangePassword(5, false);
    }

    @Test
    void passwordActualIncorrectaNoCambia() {
        when(proyectoRepository.getPasswordHash(5)).thenReturn(Optional.of(encoder.encode("Temporal23")));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.changePassword(CLIENTE, 5, change("otra", "MiClaveNueva1")));
        assertEquals("La contraseña actual no es correcta", e.getMessage());
        verify(proyectoRepository, never()).updatePasswordHash(anyInt(), anyString());
    }

    @Test
    void laNuevaDebeSerDistinta() {
        when(proyectoRepository.getPasswordHash(5)).thenReturn(Optional.of(encoder.encode("Temporal23")));

        assertThrows(BusinessException.class, () -> service.changePassword(CLIENTE, 5, change("Temporal23", "Temporal23")));
    }

    @Test
    void nadieCambiaLaPasswordDeOtro() {
        assertThrows(AccessDeniedException.class, () -> service.changePassword(ADMIN, 5, change("x", "MiClaveNueva1")));
        verify(proyectoRepository, never()).getPasswordHash(anyInt());
    }
}
