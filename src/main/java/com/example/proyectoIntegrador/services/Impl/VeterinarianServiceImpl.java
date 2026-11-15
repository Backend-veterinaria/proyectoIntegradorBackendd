package com.example.proyectoIntegrador.services.Impl;

import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.exceptions.UserAlreadyExistsException;
import com.example.proyectoIntegrador.models.RegisterDTO;
import com.example.proyectoIntegrador.models.VeterinarianCreateDTO;
import com.example.proyectoIntegrador.models.VeterinarianInfo;
import com.example.proyectoIntegrador.models.VeterinarianUpdateDTO;
import com.example.proyectoIntegrador.repository.ProyectoRepository;
import com.example.proyectoIntegrador.repository.VeterinarianRepository;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.services.VeterinarianService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VeterinarianServiceImpl implements VeterinarianService {

    private static final String NOT_FOUND = "El veterinario no existe";

    private final VeterinarianRepository veterinarianRepository;
    private final ProyectoRepository proyectoRepository;
    private final PasswordEncoder passwordEncoder;

    // Transaccional: si falla cualquier paso (usuario, contraseña, personal o medico) no queda
    // nada creado a medias.
    @Override
    @Transactional
    public VeterinarianInfo create(VeterinarianCreateDTO dto) {
        log.debug("start create veterinarian");

        if (proyectoRepository.existsUsernameOrEmail(dto.getUsername(), dto.getEmail())) {
            throw new UserAlreadyExistsException("Ya existe un usuario con ese username o email");
        }

        int rolVeterinario = proyectoRepository.getRoleId(Role.VETERINARIO)
                .orElseThrow(() -> new RuntimeException("No existe el rol veterinario en la tabla rol"));

        // 1. Usuario para iniciar sesión (tabla cliente) con rol VETERINARIO
        RegisterDTO user = new RegisterDTO();
        user.setNombre(dto.getNombre());
        user.setApellido(dto.getApellido());
        user.setTelefono(dto.getTelefono());
        // La dirección es opcional para el personal; se guarda vacía por si la columna no admite nulos
        user.setDireccion(dto.getDireccion() == null ? "" : dto.getDireccion());
        user.setEmail(dto.getEmail());
        user.setUsername(dto.getUsername());
        user.setFechaRegistro(LocalDate.now());

        proyectoRepository.registerUser(user, rolVeterinario)
                .orElseThrow(() -> new RuntimeException("No se pudo crear el usuario del veterinario"));
        int clienteId = proyectoRepository.getIdUser(dto.getUsername())
                .orElseThrow(() -> new RuntimeException("No se encontro el usuario recien creado"));
        proyectoRepository.savePasswordUser(clienteId, passwordEncoder.encode(dto.getPassword()))
                .orElseThrow(() -> new RuntimeException("No se pudo guardar la contraseña del veterinario"));

        // 2. Personal y medico, vinculados al usuario
        int personalId = veterinarianRepository.insertPersonal(dto.getNombre(), dto.getApellido(), rolVeterinario, clienteId)
                .orElseThrow(() -> new RuntimeException("No se pudo crear el registro de personal"));
        int medicoId = veterinarianRepository.insertMedico(personalId)
                .orElseThrow(() -> new RuntimeException("No se pudo crear el registro de medico"));

        log.info("Veterinario creado: medico_id={}, personal_id={}, cliente_id={}", medicoId, personalId, clienteId);
        return get(medicoId);
    }

    @Override
    public List<VeterinarianInfo> list(boolean includeInactive) {
        return veterinarianRepository.findAll(includeInactive);
    }

    @Override
    public VeterinarianInfo get(int medicoId) {
        return veterinarianRepository.findById(medicoId)
                .orElseThrow(() -> new BusinessException(NOT_FOUND));
    }

    @Override
    @Transactional
    public VeterinarianInfo update(int medicoId, VeterinarianUpdateDTO dto) {
        VeterinarianInfo current = get(medicoId);

        if (veterinarianRepository.existsEmailForOtherUser(dto.getEmail(), current.getClienteId())) {
            throw new BusinessException("El email ya esta registrado por otro usuario");
        }

        veterinarianRepository.updatePersonal(current.getPersonalId(), dto.getNombre(), dto.getApellido())
                .orElseThrow(() -> new RuntimeException("No se pudo actualizar el personal"));

        // Los veterinarios creados antes de VET-01 pueden no tener usuario vinculado
        if (current.getClienteId() != null) {
            veterinarianRepository.updateUser(current.getClienteId(), dto.getNombre(), dto.getApellido(),
                            dto.getTelefono(), dto.getEmail(), dto.getDireccion())
                    .orElseThrow(() -> new RuntimeException("No se pudo actualizar el usuario del veterinario"));
        }

        return get(medicoId);
    }

    @Override
    public VeterinarianInfo changeStatus(int medicoId, boolean activo) {
        VeterinarianInfo current = get(medicoId);

        veterinarianRepository.updateStatus(current.getPersonalId(), activo)
                .orElseThrow(() -> new RuntimeException("No se pudo cambiar el estado del veterinario"));

        log.info("Veterinario {} {}", medicoId, activo ? "activado" : "inactivado");
        return get(medicoId);
    }
}
