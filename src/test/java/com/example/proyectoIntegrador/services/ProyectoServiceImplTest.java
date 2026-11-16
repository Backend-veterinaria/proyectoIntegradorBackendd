package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.exceptions.UserAlreadyExistsException;
import com.example.proyectoIntegrador.models.InfoUser;
import com.example.proyectoIntegrador.models.LoginDTO;
import com.example.proyectoIntegrador.models.RegisterDTO;
import com.example.proyectoIntegrador.repository.ProyectoRepository;
import com.example.proyectoIntegrador.repository.VeterinarianRepository;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.services.Impl.ProyectoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProyectoServiceImplTest {

    // Costo 4 (el mínimo) para que las pruebas sean rápidas
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Mock
    private ProyectoRepository proyectoRepository;

    @Mock
    private VeterinarianRepository veterinarianRepository;

    private ProyectoServiceImpl proyectoService;

    @BeforeEach
    void setUp() {
        proyectoService = new ProyectoServiceImpl(proyectoRepository, passwordEncoder, veterinarianRepository);
    }

    private void givenVeterinarian(boolean active) {
        InfoUser user = new InfoUser();
        user.setIdClient("9");
        user.setRole("VETERINARIO");
        when(proyectoRepository.validateUser("dr.perez")).thenReturn(Optional.of(user));
        when(proyectoRepository.getPasswordHash(9)).thenReturn(Optional.of(passwordEncoder.encode("secreta123")));
        when(proyectoRepository.isUserActive(9)).thenReturn(true);
        when(veterinarianRepository.isStaffActive(9)).thenReturn(active);
    }

    @Test
    void veterinarioInactivoNoPuedeIniciarSesion() {
        givenVeterinarian(false);

        BusinessException e = assertThrows(BusinessException.class,
                () -> proyectoService.loginService(login("dr.perez", "secreta123")));
        assertEquals("Usuario inactivo. Comuniquese con el administrador", e.getMessage());
    }

    @Test
    void veterinarioActivoPuedeIniciarSesion() {
        givenVeterinarian(true);

        assertDoesNotThrow(() -> proyectoService.loginService(login("dr.perez", "secreta123")));
    }

    @Test
    void veterinarioInactivoConPasswordIncorrectaNoRevelaQueEstaInactivo() {
        InfoUser user = new InfoUser();
        user.setIdClient("9");
        user.setRole("VETERINARIO");
        when(proyectoRepository.validateUser("dr.perez")).thenReturn(Optional.of(user));
        when(proyectoRepository.getPasswordHash(9)).thenReturn(Optional.of(passwordEncoder.encode("secreta123")));

        BusinessException e = assertThrows(BusinessException.class,
                () -> proyectoService.loginService(login("dr.perez", "mala")));
        assertEquals("Usuario o contraseña incorrectos", e.getMessage());
        verify(veterinarianRepository, never()).isStaffActive(anyInt());
    }

    @Test
    void clienteNoConsultaElEstadoDePersonal() {
        givenUserWithStoredPassword(passwordEncoder.encode("secreta123"));

        assertDoesNotThrow(() -> proyectoService.loginService(login("ana", "secreta123")));
        verify(veterinarianRepository, never()).isStaffActive(anyInt());
    }

    private void givenUserWithStoredPassword(String stored) {
        InfoUser user = new InfoUser();
        user.setIdClient("5");
        when(proyectoRepository.validateUser("ana")).thenReturn(Optional.of(user));
        when(proyectoRepository.getPasswordHash(5)).thenReturn(Optional.ofNullable(stored));
        lenient().when(proyectoRepository.isUserActive(5)).thenReturn(true);
    }

    private static LoginDTO login(String username, String password) {
        LoginDTO login = new LoginDTO();
        login.setUsername(username);
        login.setPassword(password);
        return login;
    }

    @Test
    void loginConHashBcryptCorrecto() {
        givenUserWithStoredPassword(passwordEncoder.encode("secreta123"));

        assertDoesNotThrow(() -> proyectoService.loginService(login("ana", "secreta123")));
        verify(proyectoRepository, never()).updatePasswordHash(anyInt(), anyString());
    }

    @Test
    void loginConHashBcryptYPasswordIncorrectaEsRechazado() {
        givenUserWithStoredPassword(passwordEncoder.encode("secreta123"));

        assertThrows(RuntimeException.class, () -> proyectoService.loginService(login("ana", "mala")));
    }

    @Test
    void loginConPasswordAntiguaEnTextoPlanoLaMigraABcrypt() {
        givenUserWithStoredPassword("vieja123");
        when(proyectoRepository.updatePasswordHash(eq(5), anyString())).thenReturn(Optional.of(1));

        assertDoesNotThrow(() -> proyectoService.loginService(login("ana", "vieja123")));

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(proyectoRepository).updatePasswordHash(eq(5), hash.capture());
        assertNotEquals("vieja123", hash.getValue());
        assertTrue(passwordEncoder.matches("vieja123", hash.getValue()));
    }

    @Test
    void loginConPasswordAntiguaIncorrectaEsRechazadoYNoMigra() {
        givenUserWithStoredPassword("vieja123");

        assertThrows(RuntimeException.class, () -> proyectoService.loginService(login("ana", "mala")));
        verify(proyectoRepository, never()).updatePasswordHash(anyInt(), anyString());
    }

    @Test
    void loginPermitidoAunqueFalleLaMigracion() {
        givenUserWithStoredPassword("vieja123");
        when(proyectoRepository.updatePasswordHash(eq(5), anyString())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> proyectoService.loginService(login("ana", "vieja123")));
    }

    @Test
    void loginSinPasswordAlmacenadaEsRechazado() {
        givenUserWithStoredPassword(null);

        assertThrows(RuntimeException.class, () -> proyectoService.loginService(login("ana", "lo-que-sea")));
    }

    @Test
    void usuarioInexistenteYPasswordIncorrectaDanElMismoMensaje() {
        when(proyectoRepository.validateUser("nadie")).thenReturn(Optional.empty());
        givenUserWithStoredPassword(passwordEncoder.encode("secreta123"));

        BusinessException noExiste = assertThrows(BusinessException.class,
                () -> proyectoService.loginService(login("nadie", "x")));
        BusinessException malPassword = assertThrows(BusinessException.class,
                () -> proyectoService.loginService(login("ana", "mala")));

        assertEquals("Usuario o contraseña incorrectos", noExiste.getMessage());
        assertEquals(noExiste.getMessage(), malPassword.getMessage());
    }

    @Test
    void clienteInactivoNoPuedeIniciarSesion() {
        InfoUser user = new InfoUser();
        user.setIdClient("5");
        when(proyectoRepository.validateUser("ana")).thenReturn(Optional.of(user));
        when(proyectoRepository.getPasswordHash(5)).thenReturn(Optional.of(passwordEncoder.encode("secreta123")));
        when(proyectoRepository.isUserActive(5)).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class,
                () -> proyectoService.loginService(login("ana", "secreta123")));
        assertEquals("Usuario inactivo. Comuniquese con el administrador", e.getMessage());
    }

    @Test
    void clienteInactivoConPasswordIncorrectaNoRevelaQueEstaInactivo() {
        InfoUser user = new InfoUser();
        user.setIdClient("5");
        when(proyectoRepository.validateUser("ana")).thenReturn(Optional.of(user));
        when(proyectoRepository.getPasswordHash(5)).thenReturn(Optional.of(passwordEncoder.encode("secreta123")));

        BusinessException e = assertThrows(BusinessException.class,
                () -> proyectoService.loginService(login("ana", "mala")));
        assertEquals("Usuario o contraseña incorrectos", e.getMessage());
        verify(proyectoRepository, never()).isUserActive(anyInt());
    }

    @Test
    void loginDemoFuncionaSinUsuarioEnBd() {
        when(proyectoRepository.validateUser("demo")).thenReturn(Optional.empty());

        InfoUser demo = proyectoService.loginService(login("demo", "demo123"));
        assertEquals("1", demo.getIdClient());
        assertEquals("ADMIN", demo.getRole());
    }

    @Test
    void registroGuardaLaPasswordCifrada() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("ana");
        dto.setEmail("ana@correo.com");
        dto.setPassword("secreta123");
        when(proyectoRepository.existsUsernameOrEmail("ana", "ana@correo.com")).thenReturn(false);
        when(proyectoRepository.getRoleId(Role.CLIENTE)).thenReturn(Optional.of(3));
        when(proyectoRepository.registerUser(any(), eq(3))).thenReturn(Optional.of(1));
        when(proyectoRepository.getIdUser("ana")).thenReturn(Optional.of(5));
        when(proyectoRepository.savePasswordUser(eq(5), anyString())).thenReturn(Optional.of(1));

        proyectoService.registerUser(dto);

        ArgumentCaptor<String> saved = ArgumentCaptor.forClass(String.class);
        verify(proyectoRepository).savePasswordUser(eq(5), saved.capture());
        assertNotEquals("secreta123", saved.getValue());
        assertTrue(passwordEncoder.matches("secreta123", saved.getValue()));
    }

    @Test
    void registroSinRolClienteEnBdFallaYNoInserta() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("ana");
        dto.setEmail("ana@correo.com");
        when(proyectoRepository.existsUsernameOrEmail("ana", "ana@correo.com")).thenReturn(false);
        when(proyectoRepository.getRoleId(Role.CLIENTE)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> proyectoService.registerUser(dto));
        verify(proyectoRepository, never()).registerUser(any(), anyInt());
    }

    @Test
    void registroDuplicadoLanzaExcepcionYNoInserta() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("ana");
        dto.setEmail("ana@correo.com");
        when(proyectoRepository.existsUsernameOrEmail("ana", "ana@correo.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> proyectoService.registerUser(dto));
        verify(proyectoRepository, never()).registerUser(any(), anyInt());
    }
}
