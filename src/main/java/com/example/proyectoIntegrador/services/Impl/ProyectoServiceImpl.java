package com.example.proyectoIntegrador.services.Impl;

import com.example.proyectoIntegrador.exceptions.UserAlreadyExistsException;
import com.example.proyectoIntegrador.models.InfoUser;
import com.example.proyectoIntegrador.models.LoginDTO;
import com.example.proyectoIntegrador.models.RegisterDTO;
import com.example.proyectoIntegrador.models.ResponseRegister;
import com.example.proyectoIntegrador.repository.ProyectoRepository;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.services.ProyectoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProyectoServiceImpl implements ProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final PasswordEncoder passwordEncoder;

    // Hash BCrypt: $2a$, $2b$ o $2y$, costo de 2 dígitos y 53 caracteres (sal + hash)
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    // TODO: ELIMINAR antes de cualquier despliegue. Usuario de demostración quemado para
    //  ingresar a la app sin crear usuarios. Si existe un cliente con este username en la BD
    //  se usan sus datos (para que tenga citas y mascotas); si no, se devuelve DEMO_USER_ID con
    //  rol ADMIN, para poder recorrer todas las pantallas.
    private static final String DEMO_USERNAME = "demo";
    private static final String DEMO_PASSWORD = "demo123";
    private static final String DEMO_USER_ID = "1";

    @Override
    public InfoUser loginService(LoginDTO loginDTO) {

        // TODO: ELIMINAR antes de cualquier despliegue (acceso de demostración).
        if (DEMO_USERNAME.equals(loginDTO.getUsername()) && DEMO_PASSWORD.equals(loginDTO.getPassword())) {
            log.warn("Login con usuario de demostración quemado");
            return proyectoRepository.validateUser(DEMO_USERNAME).orElseGet(() -> {
                InfoUser demo = new InfoUser();
                demo.setIdClient(DEMO_USER_ID);
                demo.setName("Usuario");
                demo.setLastName("Demo");
                demo.setUsername(DEMO_USERNAME);
                demo.setRole(Role.ADMIN.name());
                return demo;
            });
        }

        var infoUser = proyectoRepository.validateUser(loginDTO.getUsername())
                .orElseThrow(() -> {
                    log.debug("Error. El usuario no esta registrado");
                    return new RuntimeException("Error. El usuario no esta registrado");
                });

        int userId = Integer.parseInt(infoUser.getIdClient());
        String stored = proyectoRepository.getPasswordHash(userId).orElse(null);

        if (!passwordMatches(userId, loginDTO.getPassword(), stored)) {
            log.debug("Error. Contraseña no valida");
            throw new RuntimeException("Error. Contraseña no valida");
        }

        return infoUser;

    }

    /**
     * Verifica la contraseña contra lo almacenado. Si lo almacenado es un hash BCrypt se usa
     * matches(); si no, es una contraseña antigua en texto plano: se compara directamente y,
     * si coincide, se reemplaza por su hash BCrypt (migración al iniciar sesión).
     */
    private boolean passwordMatches(int userId, String rawPassword, String stored) {
        if (stored == null || rawPassword == null) {
            return false;
        }

        if (BCRYPT_PATTERN.matcher(stored).matches()) {
            return passwordEncoder.matches(rawPassword, stored);
        }

        // TODO: eliminar esta rama cuando todas las contraseñas estén migradas a BCrypt.
        boolean matches = MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8), rawPassword.getBytes(StandardCharsets.UTF_8));

        if (matches) {
            proyectoRepository.updatePasswordHash(userId, passwordEncoder.encode(rawPassword))
                    .ifPresentOrElse(
                            rows -> log.info("Contraseña del usuario {} migrada a BCrypt", userId),
                            // El login sigue siendo válido; se intentará migrar de nuevo en el próximo inicio de sesión
                            () -> log.warn("No se pudo migrar a BCrypt la contraseña del usuario {}", userId));
        }

        return matches;
    }

    // Transaccional: si falla guardar la contraseña o la sesión, se revierte también el cliente
    // (cualquier RuntimeException que salga del método hace rollback).
    @Override
    @Transactional
    public ResponseRegister registerUser(RegisterDTO registerDTO) {
        Random random = new Random();
        ResponseRegister responseRegister = new ResponseRegister();

        log.debug("start registerUser");

        if (proyectoRepository.existsUsernameOrEmail(registerDTO.getUsername(), registerDTO.getEmail())) {
            log.debug("Error. Ya existe un usuario con ese username o email");
            throw new UserAlreadyExistsException("Ya existe un usuario con ese username o email");
        }

        try {

            // Obtener la fecha de hoy
            LocalDate today = LocalDate.now();

            // Crear un formateador para el nuevo formato
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");  // Cambiar a formato yyyyMMdd

            // Formatear la fecha en el nuevo formato
            today.format(formatter);

            //Registro de solo usuarios
            registerDTO.setFechaRegistro(LocalDate.parse(today.format(formatter)));
            // El registro público siempre crea clientes; administradores y veterinarios no se
            // pueden auto-registrar (se ignora cualquier rol que envíe el frontend).
            int rolCliente = proyectoRepository.getRoleId(Role.CLIENTE)
                    .orElseThrow(() -> new RuntimeException("No existe el rol cliente en la tabla rol"));

            proyectoRepository.registerUser(registerDTO, rolCliente)
                    .orElseThrow(() -> new RuntimeException("Error al registrar usuario"));

            var idUser = proyectoRepository.getIdUser(registerDTO.getUsername()).orElseThrow(() -> new RuntimeException("Error. No se encontro el usuario"));

            proyectoRepository.savePasswordUser(idUser, passwordEncoder.encode(registerDTO.getPassword())).orElseThrow(() -> new RuntimeException("Error, al guardar la contraseña"));

            int numeroAleatorio = random.nextInt(100); // 0 a 99
            proyectoRepository.setSessiones(numeroAleatorio, idUser);
            responseRegister.setSession(numeroAleatorio);

            return responseRegister;

        } catch (Exception e) {
            log.debug("Error al crear usuario: {}", e.getMessage());
            throw new RuntimeException();
        }

    }

}
