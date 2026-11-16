package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.models.ClientInfo;
import com.example.proyectoIntegrador.models.ClientUpdateDTO;
import com.example.proyectoIntegrador.security.AuthenticatedUser;

import java.util.List;

/**
 * Gestión de propietarios (CIT-03, RF-03).
 */
public interface ClientService {

    /**
     * El cliente solo puede consultar su propio perfil; el personal, cualquiera.
     *
     * @throws org.springframework.security.access.AccessDeniedException si un cliente consulta otro perfil
     */
    ClientInfo get(AuthenticatedUser user, int clienteId);

    /**
     * Edita los datos de contacto (CPA-05). Mismos permisos que {@link #get}.
     */
    ClientInfo update(AuthenticatedUser user, int clienteId, ClientUpdateDTO dto);

    /**
     * Lista de propietarios para el personal.
     *
     * @param includeInactive si es false, solo los activos
     * @param search          opcional: busca por nombre, apellido, username o email
     */
    List<ClientInfo> list(boolean includeInactive, String search);

    /**
     * Activa o inactiva un propietario (CPA-06). Un propietario inactivo conserva su historial, pero no
     * puede iniciar sesión ni se le pueden agendar nuevas citas. El personal se gestiona en VET-01.
     */
    ClientInfo changeStatus(AuthenticatedUser user, int clienteId, boolean activo);
}
