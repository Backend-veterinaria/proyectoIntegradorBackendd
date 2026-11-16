package com.example.proyectoIntegrador.repository;


import com.example.proyectoIntegrador.models.InfoUser;
import com.example.proyectoIntegrador.models.RegisterClient;
import com.example.proyectoIntegrador.models.RegisterDTO;
import com.example.proyectoIntegrador.security.Role;

import java.util.Optional;

public interface ProyectoRepository {

    /**
     * Registra un nuevo usuario en la base de datos utilizando el objeto RegisterDTO proporcionado.
     *
     * @param registerDTO el objeto de transferencia de datos que contiene los detalles de registro del usuario
     * @param rolId       el id (tabla rol) del rol que se asigna al usuario
     * @return un Optional que contiene el número de filas afectadas si la operación es exitosa,
     * o un Optional vacío si ocurre una excepción
     */
    Optional<Integer> registerUser(RegisterDTO registerDTO, int rolId);

    /**
     * Busca en la tabla rol el id del rol dado, por su descripción.
     *
     * @param role el rol a buscar
     * @return un Optional con el rol_id, o vacío si no existe un rol con esa descripción
     */
    Optional<Integer> getRoleId(Role role);

    /**
     * Obtiene la información de un cliente basado en los datos proporcionados en el objeto RegisterDTO.
     *
     * @param registerDTO Objeto que contiene los datos del cliente a buscar.
     * @return Un Optional que contiene un objeto RegisterClient si se encuentra el cliente, o un Optional vacío si no se encuentra.
     */
    Optional<RegisterClient> getInfoUser(RegisterDTO registerDTO);

    /**
     * Inserta un nuevo registro de sesión en la tabla 'sesiones' con el código de sesión y el ID de usuario proporcionados.
     * La sesión se marca como activa y se utiliza la marca de tiempo actual como fecha de inicio.
     *
     * @param codeSession el token de sesión que se almacenará.
     * @param userId      el ID del usuario asociado con la sesión.
     * @return un Optional que contiene el número de filas afectadas por la operación de inserción,
     * o un Optional vacío si ocurre un error.
     */
    Optional<Integer> setSessiones(int codeSession, int userId);

    /**
     * Indica si ya existe un cliente con el username o el email dados (sin distinguir mayúsculas).
     * A diferencia de los demás métodos, no captura las excepciones: si la consulta falla,
     * el registro debe fallar en lugar de asumir que el usuario no existe.
     *
     * @param username el nombre de usuario a verificar
     * @param email    el correo a verificar
     * @return true si ya existe un cliente con ese username o email
     */
    boolean existsUsernameOrEmail(String username, String email);

    /**
     * Indica si el usuario está activo (cliente.estado_cliente; un valor nulo cuenta como activo).
     * Un usuario que no existe no está activo. No captura excepciones.
     */
    boolean isUserActive(int clienteId);

    /**
     * Recupera el ID de un usuario basado en su nombre de usuario.
     *
     * @param userName el nombre de usuario del cual se desea obtener el ID
     * @return un Optional que contiene el ID del usuario si se encuentra, o un Optional vacío si no se encuentra
     */
    Optional<Integer> getIdUser(String userName);

    /**
     * Valida y recupera la información del usuario basado en el nombre de usuario proporcionado.
     *
     * @param username el nombre de usuario del cual se desea validar y obtener información
     * @return un Optional que contiene el objeto InfoUser con los detalles del usuario si se encuentra,
     * o un Optional vacío si el usuario no existe o si ocurre un error
     */
    Optional<InfoUser> validateUser(String username);

    /**
     * Obtiene la contraseña almacenada (hash BCrypt o, en usuarios antiguos, texto plano) del usuario.
     *
     * @param userId el ID del usuario
     * @return un Optional con el valor guardado en password_hash, o vacío si no existe o hay un error
     */
    Optional<String> getPasswordHash(int userId);

    /**
     * Reemplaza la contraseña almacenada del usuario. Se usa para migrar a BCrypt las contraseñas
     * antiguas guardadas en texto plano.
     *
     * @param userId       el ID del usuario
     * @param passwordHash el nuevo hash BCrypt
     * @return un Optional con el número de filas actualizadas, o vacío si ocurre un error
     */
    Optional<Integer> updatePasswordHash(int userId, String passwordHash);

    /**
     * Guarda la contraseña de un usuario en la base de datos.
     *
     * @param idClient el ID del cliente cuya contraseña se va a guardar
     * @param password el hash BCrypt de la contraseña (nunca la contraseña en texto plano)
     * @return un Optional que contiene el número de filas afectadas si la operación es exitosa,
     * o un Optional vacío si ocurre un error
     */
    public Optional<Integer> savePasswordUser(int idClient, String password);

}
