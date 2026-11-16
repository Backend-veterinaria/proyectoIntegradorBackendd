package com.example.proyectoIntegrador.repository;

import com.example.proyectoIntegrador.models.ClientInfo;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de los propietarios (tabla cliente) para CIT-03. Ningún método captura
 * excepciones: si la consulta falla, la operación debe fallar.
 */
public interface ClientRepository {

    /**
     * @return el usuario de la tabla cliente con ese id (propietario o personal), o vacío si no existe
     */
    Optional<ClientInfo> findById(int clienteId);

    /**
     * Propietarios (usuarios con rol cliente, no personal), ordenados por apellido y nombre.
     *
     * @param includeInactive si es false, solo los activos
     * @param search          opcional: filtra por nombre, apellido, username o email (contiene, sin mayúsculas)
     */
    List<ClientInfo> findOwners(boolean includeInactive, String search);

    /**
     * Indica si el email ya lo usa un usuario distinto al indicado (sin distinguir mayúsculas).
     */
    boolean existsEmailForOtherUser(String email, int clienteId);

    /**
     * @return el número de filas actualizadas
     */
    int update(int clienteId, String nombre, String apellido, String direccion, String telefono, String email);

    /**
     * Activa o inactiva el usuario (cliente.estado_cliente).
     *
     * @return el número de filas actualizadas
     */
    int updateStatus(int clienteId, boolean activo);
}
