package com.example.proyectoIntegrador.services.Impl;

import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.ClientInfo;
import com.example.proyectoIntegrador.models.ClientUpdateDTO;
import com.example.proyectoIntegrador.repository.ClientRepository;
import com.example.proyectoIntegrador.security.AuthenticatedUser;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.security.SecurityUtils;
import com.example.proyectoIntegrador.services.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private static final String NOT_FOUND = "El cliente no existe";

    private final ClientRepository clientRepository;

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
