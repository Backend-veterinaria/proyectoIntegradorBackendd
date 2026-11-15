package com.example.proyectoIntegrador.repository;

import com.example.proyectoIntegrador.models.InfoMascota;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de las mascotas. Requiere la columna mascota.activo (sql/CIT-02_gestion_mascotas.sql).
 */
public interface PetsRepository {

    Optional<Boolean> validateExistPet(String nombre, int edad, String genero, Integer clienteId);

    Optional<Boolean> addPet(Integer clientId, String petCodigo, String petName, Integer petAge, String petGender, Integer razaId);

    /**
     * @param includeInactive si es false, solo devuelve las mascotas activas
     * @return las mascotas del cliente, o vacío si no tiene ninguna o hay un error
     */
    Optional<List<InfoMascota>> getListPetsByUser(int idClient, boolean includeInactive);

    /**
     * @return la mascota (con su dueño y estado), o vacío si no existe. No captura excepciones.
     */
    Optional<InfoMascota> findById(int mascotaId);

    /**
     * Indica si el cliente tiene otra mascota (distinta de excludeMascotaId) con el mismo nombre,
     * edad y género. No captura excepciones.
     */
    boolean existsOtherPet(String nombre, int edad, String genero, int clienteId, int excludeMascotaId);

    /**
     * @return el número de mascotas actualizadas. No captura excepciones.
     */
    int updatePet(int mascotaId, String nombre, int edad, String genero, int razaId);

    /**
     * @return el número de mascotas actualizadas. No captura excepciones.
     */
    int updateStatus(int mascotaId, boolean activo);
}
