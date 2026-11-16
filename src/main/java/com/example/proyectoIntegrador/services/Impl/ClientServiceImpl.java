package com.example.proyectoIntegrador.services.Impl;

import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.exceptions.UserAlreadyExistsException;
import com.example.proyectoIntegrador.models.ClientCreateDTO;
import com.example.proyectoIntegrador.models.ClientInfo;
import com.example.proyectoIntegrador.models.ClientTemporaryPassword;
import com.example.proyectoIntegrador.models.ClientUpdateDTO;
import com.example.proyectoIntegrador.models.PasswordChangeDTO;
import com.example.proyectoIntegrador.models.RegisterDTO;
import com.example.proyectoIntegrador.repository.ClientRepository;
import com.example.proyectoIntegrador.repository.ProyectoRepository;
import com.example.proyectoIntegrador.security.AuthenticatedUser;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.security.SecurityUtils;
import com.example.proyectoIntegrador.services.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private static final String NOT_FOUND = "El cliente no existe";

    private final ClientRepository clientRepository;
    private final ProyectoRepository proyectoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ClientInfo get(AuthenticatedUser user, int clienteId) {
        requireAccess(user, clienteId);
        return find(clienteId);
    }

    @Override
    public ClientInfo update(AuthenticatedUser user, int clienteId, ClientUpdateDTO dto) {
        requireAccess(user, clienteId);
        find(clienteId);

        if (clientRepository.existsEmailForOtherUser(dto.getEmail(), clienteId)) {
            throw new BusinessException("El email ya esta registrado por otro usuario");
        }

        if (clientRepository.update(clienteId, dto.getNombre().trim(), dto.getApellido().trim(),
                dto.getDireccion().trim(), dto.getTelefono().trim(), dto.getEmail().trim()) == 0) {
            throw new BusinessException(NOT_FOUND);
        }

        return find(clienteId);
    }

    @Override
    public List<ClientInfo> list(boolean includeInactive, String search) {
        return clientRepository.findOwners(includeInactive, search);
    }

    @Override
    public ClientInfo changeStatus(AuthenticatedUser user, int clienteId, boolean activo) {
        if (clienteId == user.userId()) {
            throw new BusinessException("No puede cambiar el estado de su propia cuenta");
        }

        ClientInfo client = find(clienteId);

        // El personal se activa/inactiva desde la gestión de veterinarios (personal.activo)
        if (Role.fromName(client.getRole()).isStaff()) {
            throw new BusinessException("El usuario es personal de la veterinaria: use la gestion de veterinarios");
        }

        if (clientRepository.updateStatus(clienteId, activo) == 0) {
            throw new BusinessException(NOT_FOUND);
        }

        log.info("Cliente {} {} por el usuario {}", clienteId, activo ? "activado" : "inactivado", user.userId());
        return find(clienteId);
    }

    /**
     * El personal registra un propietario desde la clínica (CIT-06). Se le asigna una contraseña
     * temporal, que se devuelve una sola vez, y queda marcado para cambiarla en su primer inicio de sesión.
     */
    @Override
    @Transactional
    public ClientTemporaryPassword create(AuthenticatedUser user, ClientCreateDTO dto) {
        requireStaff(user);

        String username = dto.getUsername().trim();
        String email = dto.getEmail().trim();
        if (proyectoRepository.existsUsernameOrEmail(username, email)) {
            throw new UserAlreadyExistsException("Ya existe un usuario con ese username o email");
        }

        int rolCliente = proyectoRepository.getRoleId(Role.CLIENTE)
                .orElseThrow(() -> new RuntimeException("No existe el rol cliente en la tabla rol"));

        RegisterDTO registro = new RegisterDTO();
        registro.setNombre(dto.getNombre().trim());
        registro.setApellido(dto.getApellido().trim());
        registro.setDireccion(dto.getDireccion() == null || dto.getDireccion().isBlank() ? null : dto.getDireccion().trim());
        registro.setTelefono(dto.getTelefono().trim());
        registro.setEmail(email);
        registro.setUsername(username);
        registro.setFechaRegistro(LocalDate.now());

        proyectoRepository.registerUser(registro, rolCliente)
                .orElseThrow(() -> new RuntimeException("No se pudo registrar el cliente"));
        int clienteId = proyectoRepository.getIdUser(username)
                .orElseThrow(() -> new RuntimeException("No se encontro el cliente recien registrado"));

        String temporal = generateTemporaryPassword();
        proyectoRepository.savePasswordUser(clienteId, passwordEncoder.encode(temporal))
                .orElseThrow(() -> new RuntimeException("No se pudo guardar la contraseña"));
        if (proyectoRepository.setMustChangePassword(clienteId, true) == 0) {
            throw new RuntimeException("No se pudo marcar la contraseña como temporal");
        }

        log.info("Cliente {} registrado por el usuario {}", clienteId, user.userId());
        return new ClientTemporaryPassword(find(clienteId), temporal);
    }

    /**
     * El personal le da una nueva contraseña temporal a un propietario que olvidó la suya (CIT-06).
     * La del personal no se restablece aquí.
     */
    @Override
    @Transactional
    public ClientTemporaryPassword resetPassword(AuthenticatedUser user, int clienteId) {
        requireStaff(user);

        ClientInfo client = find(clienteId);
        if (Role.fromName(client.getRole()).isStaff()) {
            throw new BusinessException("La contraseña del personal no se restablece desde la gestion de propietarios");
        }

        String temporal = generateTemporaryPassword();
        proyectoRepository.updatePasswordHash(clienteId, passwordEncoder.encode(temporal))
                .filter(rows -> rows > 0)
                .orElseThrow(() -> new RuntimeException("No se pudo actualizar la contraseña"));
        proyectoRepository.setMustChangePassword(clienteId, true);

        log.info("Contraseña del cliente {} restablecida por el usuario {}", clienteId, user.userId());
        return new ClientTemporaryPassword(client, temporal);
    }

    /**
     * Cada usuario cambia su propia contraseña, dando la actual (CIT-06). Nadie cambia la de otro:
     * el personal usa {@link #resetPassword}.
     */
    @Override
    @Transactional
    public void changePassword(AuthenticatedUser user, int clienteId, PasswordChangeDTO dto) {
        if (clienteId != user.userId()) {
            throw new AccessDeniedException("Solo puede cambiar su propia contraseña");
        }

        String stored = proyectoRepository.getPasswordHash(clienteId).orElse(null);
        if (stored == null || !passwordEncoder.matches(dto.getPasswordActual(), stored)) {
            throw new BusinessException("La contraseña actual no es correcta");
        }
        if (dto.getPasswordActual().equals(dto.getPasswordNueva())) {
            throw new BusinessException("La nueva contraseña debe ser distinta de la actual");
        }

        proyectoRepository.updatePasswordHash(clienteId, passwordEncoder.encode(dto.getPasswordNueva()))
                .filter(rows -> rows > 0)
                .orElseThrow(() -> new RuntimeException("No se pudo actualizar la contraseña"));
        proyectoRepository.setMustChangePassword(clienteId, false);
        log.info("El usuario {} cambio su contraseña", clienteId);
    }

    /** Sin caracteres que se confunden al dictarla o copiarla a mano (0/O, 1/l/I). */
    private static final String TEMP_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int TEMP_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    static String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(TEMP_LENGTH);
        for (int i = 0; i < TEMP_LENGTH; i++) {
            sb.append(TEMP_ALPHABET.charAt(RANDOM.nextInt(TEMP_ALPHABET.length())));
        }
        return sb.toString();
    }

    private static void requireStaff(AuthenticatedUser user) {
        // El controlador ya lo restringe con @PreAuthorize; esto es una segunda barrera
        if (!user.role().isStaff()) {
            throw new AccessDeniedException("Solo el personal puede registrar propietarios o restablecer contraseñas");
        }
    }

    private ClientInfo find(int clienteId) {
        return clientRepository.findById(clienteId)
                .orElseThrow(() -> new BusinessException(NOT_FOUND));
    }

    private static void requireAccess(AuthenticatedUser user, int clienteId) {
        if (!SecurityUtils.canAccessClient(user, clienteId)) {
            throw new AccessDeniedException("Solo puede acceder a sus propios datos");
        }
    }
}
