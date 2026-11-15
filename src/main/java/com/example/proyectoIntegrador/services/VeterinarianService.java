package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.models.VeterinarianCreateDTO;
import com.example.proyectoIntegrador.models.VeterinarianInfo;
import com.example.proyectoIntegrador.models.VeterinarianUpdateDTO;

import java.util.List;

/**
 * Gestión de veterinarios (VET-01, RF-05).
 */
public interface VeterinarianService {

    /**
     * Crea el usuario del veterinario (rol VETERINARIO) y sus registros en personal y medico,
     * en una sola transacción.
     *
     * @throws com.example.proyectoIntegrador.exceptions.UserAlreadyExistsException si el username o email ya existen
     */
    VeterinarianInfo create(VeterinarianCreateDTO dto);

    /**
     * @param includeInactive si es false, solo devuelve los veterinarios activos
     */
    List<VeterinarianInfo> list(boolean includeInactive);

    /**
     * @throws com.example.proyectoIntegrador.exceptions.BusinessException si el veterinario no existe
     */
    VeterinarianInfo get(int medicoId);

    /**
     * Actualiza los datos del veterinario en personal y en su usuario.
     */
    VeterinarianInfo update(int medicoId, VeterinarianUpdateDTO dto);

    /**
     * Activa o inactiva el veterinario. Un veterinario inactivo no se ofrece para nuevas citas,
     * no se le pueden agendar citas y no puede iniciar sesión; conserva su historial.
     */
    VeterinarianInfo changeStatus(int medicoId, boolean activo);
}
