package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.exceptions.UserAlreadyExistsException;
import com.example.proyectoIntegrador.models.RegisterDTO;
import com.example.proyectoIntegrador.models.VeterinarianCreateDTO;
import com.example.proyectoIntegrador.models.VeterinarianInfo;
import com.example.proyectoIntegrador.models.VeterinarianUpdateDTO;
import com.example.proyectoIntegrador.repository.ProyectoRepository;
import com.example.proyectoIntegrador.repository.VeterinarianRepository;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.services.Impl.VeterinarianServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VeterinarianServiceImplTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Mock
    private VeterinarianRepository veterinarianRepository;

    @Mock
    private ProyectoRepository proyectoRepository;

    private VeterinarianServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VeterinarianServiceImpl(veterinarianRepository, proyectoRepository, passwordEncoder);
    }

    private static VeterinarianCreateDTO createDto() {
        VeterinarianCreateDTO dto = new VeterinarianCreateDTO();
        dto.setNombre("Laura");
        dto.setApellido("Perez");
        dto.setTelefono("3001234567");
        dto.setEmail("laura@vet.com");
        dto.setUsername("dr.perez");
        dto.setPassword("secreta123");
        return dto;
    }

    private static VeterinarianInfo info(int medicoId, int personalId, Integer clienteId) {
        VeterinarianInfo info = new VeterinarianInfo();
        info.setMedicoId(medicoId);
        info.setPersonalId(personalId);
        info.setClienteId(clienteId);
        info.setActivo(true);
        return info;
    }

    @Test
    void crearVeterinarioCreaUsuarioPersonalYMedicoVinculados() {
        when(proyectoRepository.existsUsernameOrEmail("dr.perez", "laura@vet.com")).thenReturn(false);
        when(proyectoRepository.getRoleId(Role.VETERINARIO)).thenReturn(Optional.of(2));
        when(proyectoRepository.registerUser(any(), eq(2))).thenReturn(Optional.of(1));
        when(proyectoRepository.getIdUser("dr.perez")).thenReturn(Optional.of(40));
        when(proyectoRepository.savePasswordUser(eq(40), anyString())).thenReturn(Optional.of(1));
        when(veterinarianRepository.insertPersonal("Laura", "Perez", 2, 40)).thenReturn(Optional.of(7));
        when(veterinarianRepository.insertMedico(7)).thenReturn(Optional.of(11));
        when(veterinarianRepository.findById(11)).thenReturn(Optional.of(info(11, 7, 40)));

        VeterinarianInfo result = service.create(createDto());

        assertEquals(11, result.getMedicoId());

        // El usuario se crea con rol veterinario y dirección vacía (es opcional)
        ArgumentCaptor<RegisterDTO> user = ArgumentCaptor.forClass(RegisterDTO.class);
        verify(proyectoRepository).registerUser(user.capture(), eq(2));
        assertEquals("dr.perez", user.getValue().getUsername());
        assertEquals("", user.getValue().getDireccion());

        // La contraseña se guarda cifrada
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(proyectoRepository).savePasswordUser(eq(40), hash.capture());
        assertTrue(passwordEncoder.matches("secreta123", hash.getValue()));
    }

    @Test
    void crearVeterinarioDuplicadoNoInsertaNada() {
        when(proyectoRepository.existsUsernameOrEmail("dr.perez", "laura@vet.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> service.create(createDto()));
        verify(proyectoRepository, never()).registerUser(any(), anyInt());
        verify(veterinarianRepository, never()).insertPersonal(anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void crearVeterinarioSinRolVeterinarioEnBdFalla() {
        when(proyectoRepository.existsUsernameOrEmail("dr.perez", "laura@vet.com")).thenReturn(false);
        when(proyectoRepository.getRoleId(Role.VETERINARIO)).thenReturn(Optional.empty());

        RuntimeException e = assertThrows(RuntimeException.class, () -> service.create(createDto()));
        // Es un error de configuración interno, no un error de negocio para mostrar al usuario
        assertTrue(!(e instanceof BusinessException));
        verify(proyectoRepository, never()).registerUser(any(), anyInt());
    }

    @Test
    void crearVeterinarioFallaSiNoSePuedeCrearElMedico() {
        when(proyectoRepository.existsUsernameOrEmail("dr.perez", "laura@vet.com")).thenReturn(false);
        when(proyectoRepository.getRoleId(Role.VETERINARIO)).thenReturn(Optional.of(2));
        when(proyectoRepository.registerUser(any(), eq(2))).thenReturn(Optional.of(1));
        when(proyectoRepository.getIdUser("dr.perez")).thenReturn(Optional.of(40));
        when(proyectoRepository.savePasswordUser(eq(40), anyString())).thenReturn(Optional.of(1));
        when(veterinarianRepository.insertPersonal("Laura", "Perez", 2, 40)).thenReturn(Optional.of(7));
        when(veterinarianRepository.insertMedico(7)).thenReturn(Optional.empty());

        // La excepción hace rollback de la transacción (@Transactional) en la aplicación real
        assertThrows(RuntimeException.class, () -> service.create(createDto()));
    }

    @Test
    void consultarVeterinarioInexistenteEsErrorDeNegocio() {
        when(veterinarianRepository.findById(99)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> service.get(99));
        assertEquals("El veterinario no existe", e.getMessage());
    }

    @Test
    void editarConEmailDeOtroUsuarioFalla() {
        when(veterinarianRepository.findById(11)).thenReturn(Optional.of(info(11, 7, 40)));
        when(veterinarianRepository.existsEmailForOtherUser("ana@correo.com", 40)).thenReturn(true);

        VeterinarianUpdateDTO dto = new VeterinarianUpdateDTO();
        dto.setNombre("Laura");
        dto.setApellido("Perez");
        dto.setTelefono("300");
        dto.setEmail("ana@correo.com");

        assertThrows(BusinessException.class, () -> service.update(11, dto));
        verify(veterinarianRepository, never()).updatePersonal(anyInt(), anyString(), anyString());
    }

    @Test
    void editarVeterinarioSinUsuarioVinculadoSoloActualizaPersonal() {
        when(veterinarianRepository.findById(11)).thenReturn(Optional.of(info(11, 7, null)));
        when(veterinarianRepository.existsEmailForOtherUser("laura@vet.com", null)).thenReturn(false);
        when(veterinarianRepository.updatePersonal(7, "Laura", "Gomez")).thenReturn(Optional.of(true));

        VeterinarianUpdateDTO dto = new VeterinarianUpdateDTO();
        dto.setNombre("Laura");
        dto.setApellido("Gomez");
        dto.setTelefono("300");
        dto.setEmail("laura@vet.com");

        service.update(11, dto);

        verify(veterinarianRepository, never()).updateUser(anyInt(), any(), any(), any(), any(), any());
    }

    @Test
    void inactivarVeterinario() {
        when(veterinarianRepository.findById(11)).thenReturn(Optional.of(info(11, 7, 40)));
        when(veterinarianRepository.updateStatus(7, false)).thenReturn(Optional.of(true));

        service.changeStatus(11, false);

        verify(veterinarianRepository).updateStatus(7, false);
    }

    @Test
    void inactivarVeterinarioInexistenteNoActualiza() {
        when(veterinarianRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.changeStatus(99, false));
        verify(veterinarianRepository, never()).updateStatus(anyInt(), anyBoolean());
    }
}
