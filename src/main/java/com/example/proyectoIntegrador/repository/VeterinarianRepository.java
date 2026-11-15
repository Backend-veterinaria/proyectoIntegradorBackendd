package com.example.proyectoIntegrador.repository;

import com.example.proyectoIntegrador.models.VeterinarianInfo;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de los veterinarios (tablas personal y medico, y su usuario en cliente).
 * Requiere las columnas personal.activo y personal.cliente_id (sql/VET-01_gestion_veterinarios.sql).
 */
public interface VeterinarianRepository {

    /**
     * Inserta el registro en personal.
     *
     * @return un Optional con el personal_id generado, o vacío si ocurre un error
     */
    Optional<Integer> insertPersonal(String nombre, String apellido, int rolId, int clienteId);

    /**
     * Inserta el registro en medico asociado al personal.
     *
     * @return un Optional con el medico_id generado, o vacío si ocurre un error
     */
    Optional<Integer> insertMedico(int personalId);

    /**
     * @param includeInactive si es false, solo devuelve los veterinarios activos
     * @return la lista de veterinarios ordenada por apellido y nombre (vacía si no hay ninguno)
     */
    List<VeterinarianInfo> findAll(boolean includeInactive);

    /**
     * @return el veterinario con ese medico_id, o vacío si no existe
     */
    Optional<VeterinarianInfo> findById(int medicoId);

    /**
     * Actualiza nombre y apellido en personal.
     *
     * @return un Optional con true si se actualizó, o vacío si no existe o hay un error
     */
    Optional<Boolean> updatePersonal(int personalId, String nombre, String apellido);

    /**
     * Actualiza los datos de contacto del usuario del veterinario en cliente. La dirección
     * solo se cambia si no es nula.
     *
     * @return un Optional con true si se actualizó, o vacío si no existe o hay un error
     */
    Optional<Boolean> updateUser(int clienteId, String nombre, String apellido, String telefono, String email, String direccion);

    /**
     * Activa o inactiva el personal.
     *
     * @return un Optional con true si se actualizó, o vacío si no existe o hay un error
     */
    Optional<Boolean> updateStatus(int personalId, boolean activo);

    /**
     * Indica si el email ya lo usa un usuario distinto al indicado (sin distinguir mayúsculas).
     * No captura excepciones: si la consulta falla, la operación debe fallar.
     */
    boolean existsEmailForOtherUser(String email, Integer clienteId);

    /**
     * Indica si el usuario puede iniciar sesión como personal: true si no está vinculado a
     * personal o si su registro de personal está activo. No captura excepciones.
     */
    boolean isStaffActive(int clienteId);

    /**
     * Busca el medico_id del veterinario vinculado a un usuario (personal.cliente_id).
     *
     * @return el medico_id, o vacío si el usuario no está vinculado a un veterinario
     */
    Optional<Integer> findMedicoIdByClienteId(int clienteId);
}
