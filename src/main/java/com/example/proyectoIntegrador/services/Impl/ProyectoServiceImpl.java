package com.example.proyectoIntegrador.services.Impl;

import com.example.proyectoIntegrador.models.InfoUser;
import com.example.proyectoIntegrador.models.LoginDTO;
import com.example.proyectoIntegrador.models.RegisterDTO;
import com.example.proyectoIntegrador.models.ResponseRegister;
import com.example.proyectoIntegrador.repository.ProyectoRepository;
import com.example.proyectoIntegrador.services.ProyectoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProyectoServiceImpl implements ProyectoService {

    private final ProyectoRepository proyectoRepository;

    // TODO: ELIMINAR antes de cualquier despliegue. Usuario de demostración quemado para
    //  ingresar a la app sin crear usuarios. Si existe un cliente con este username en la BD
    //  se usan sus datos (para que tenga citas y mascotas); si no, se devuelve DEMO_USER_ID.
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
                return demo;
            });
        }

        var infoUser = proyectoRepository.validateUser(loginDTO.getUsername())
                .orElseThrow(() -> {
                    log.debug("Error. El usuario no esta registrado");
                    return new RuntimeException("Error. El usuario no esta registrado");
                });

        proyectoRepository.validatePassword(infoUser.getIdClient(), loginDTO.getPassword())
                .orElseThrow(() -> {
                    log.debug("Error. Contraseña no valida");
                    return new RuntimeException("Error. Contraseña no valida");
                });

        return infoUser;

    }

    @Override
    public ResponseRegister registerUser(RegisterDTO registerDTO) {
        Random random = new Random();
        ResponseRegister responseRegister = new ResponseRegister();

        log.debug("start registerUser");

        try {

            // Obtener la fecha de hoy
            LocalDate today = LocalDate.now();

            // Crear un formateador para el nuevo formato
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");  // Cambiar a formato yyyyMMdd

            // Formatear la fecha en el nuevo formato
            today.format(formatter);

            //Registro de solo usuarios
            registerDTO.setFechaRegistro(LocalDate.parse(today.format(formatter)));
            proyectoRepository.registerUser(registerDTO)
                    .orElseThrow(() -> new RuntimeException("Error al registrar usuario"));

            var idUser = proyectoRepository.getIdUser(registerDTO.getUsername()).orElseThrow(() -> new RuntimeException("Error. No se encontro el usuario"));

            proyectoRepository.savePasswordUser(idUser, registerDTO.getPassword()).orElseThrow(() -> new RuntimeException("Error, al guardar la contraseña"));

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
