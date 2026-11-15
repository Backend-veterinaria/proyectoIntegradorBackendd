package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.exceptions.UserAlreadyExistsException;
import com.example.proyectoIntegrador.models.InfoUser;
import com.example.proyectoIntegrador.models.LoginDTO;
import com.example.proyectoIntegrador.models.RegisterDTO;
import com.example.proyectoIntegrador.repository.ProyectoRepository;
import com.example.proyectoIntegrador.services.Impl.ProyectoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProyectoServiceImplTest {

    @Mock
    private ProyectoRepository proyectoRepository;

    @InjectMocks
    private ProyectoServiceImpl proyectoService;

    @Test
    void registroDuplicadoLanzaExcepcionYNoInserta() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("ana");
        dto.setEmail("ana@correo.com");
        when(proyectoRepository.existsUsernameOrEmail("ana", "ana@correo.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> proyectoService.registerUser(dto));
        verify(proyectoRepository, never()).registerUser(any());
    }

    @Test
    void loginConPasswordIncorrectaEsRechazado() {
        InfoUser user = new InfoUser();
        user.setIdClient("5");
        when(proyectoRepository.validateUser("ana")).thenReturn(Optional.of(user));
        when(proyectoRepository.validatePassword("5", "mala")).thenReturn(Optional.empty());

        LoginDTO login = new LoginDTO();
        login.setUsername("ana");
        login.setPassword("mala");

        assertThrows(RuntimeException.class, () -> proyectoService.loginService(login));
    }

    @Test
    void loginDemoFuncionaSinUsuarioEnBd() {
        when(proyectoRepository.validateUser("demo")).thenReturn(Optional.empty());

        LoginDTO login = new LoginDTO();
        login.setUsername("demo");
        login.setPassword("demo123");

        assertEquals("1", proyectoService.loginService(login).getIdClient());
    }
}
