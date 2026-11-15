package com.example.proyectoIntegrador.controllers;

import com.example.proyectoIntegrador.controllers.Impl.ProyectoControllerImpl;
import com.example.proyectoIntegrador.exceptions.UserAlreadyExistsException;
import com.example.proyectoIntegrador.models.InfoUser;
import com.example.proyectoIntegrador.models.ResponseRegister;
import com.example.proyectoIntegrador.security.AuthenticatedUser;
import com.example.proyectoIntegrador.security.JwtService;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.security.SecurityConfig;
import com.example.proyectoIntegrador.services.ProyectoService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProyectoControllerImpl.class)
@Import({SecurityConfig.class, JwtService.class})
class ProyectoControllerImplTest {

    private static final String VALID_REGISTER = """
            {"nombre":"Ana","apellido":"Gomez","direccion":"Calle 1","telefono":"3001234567",
             "email":"ana@correo.com","username":"ana","password":"secreta123","rolUser":1}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProyectoService proyectoService;

    @Autowired
    private JwtService jwtService;

    @Test
    void loginCorrectoDevuelveTokenValido() throws Exception {
        InfoUser user = new InfoUser();
        user.setIdClient("5");
        user.setUsername("ana");
        when(proyectoService.loginService(any())).thenReturn(user);

        String body = mockMvc.perform(post("/integrador/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"secreta123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(28800))
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(body, "$.token");
        // Sin rol en InfoUser se asume CLIENTE
        assertEquals(new AuthenticatedUser(5, "ana", Role.CLIENTE), jwtService.parseToken(token).orElseThrow());
    }

    @Test
    void elTokenDelLoginLlevaElRolDelUsuario() throws Exception {
        InfoUser user = new InfoUser();
        user.setIdClient("9");
        user.setUsername("dr.perez");
        user.setRole("VETERINARIO");
        when(proyectoService.loginService(any())).thenReturn(user);

        String body = mockMvc.perform(post("/integrador/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dr.perez\",\"password\":\"secreta123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseObj.role").value("VETERINARIO"))
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(body, "$.token");
        assertEquals(Role.VETERINARIO, jwtService.parseToken(token).orElseThrow().role());
    }

    @Test
    void loginFallidoNoDevuelveToken() throws Exception {
        when(proyectoService.loginService(any())).thenThrow(new RuntimeException("Error. Contraseña no valida"));

        mockMvc.perform(post("/integrador/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"mala\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("1231"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void loginSinPasswordDevuelveErrorDeValidacion() throws Exception {
        mockMvc.perform(post("/integrador/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6617"))
                .andExpect(jsonPath("$.responseObj", hasItem("password: you must specify the password field")));

        verify(proyectoService, never()).loginService(any());
    }

    @Test
    void registroConDatosInvalidosDevuelveErrorDeValidacion() throws Exception {
        mockMvc.perform(post("/integrador/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Gomez","direccion":"Calle 1","telefono":"300",
                                 "email":"no-es-correo","username":"ana","password":"corta"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6617"))
                .andExpect(jsonPath("$.responseObj", hasItem("email: email is not valid")))
                .andExpect(jsonPath("$.responseObj", hasItem("password: password must have between 8 and 72 characters")));

        verify(proyectoService, never()).registerUser(any());
    }

    @Test
    void jsonMalFormadoDevuelveErrorDeValidacion() throws Exception {
        mockMvc.perform(post("/integrador/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": ["))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6617"));
    }

    @Test
    void registroDuplicadoDevuelveUserExists() throws Exception {
        when(proyectoService.registerUser(any()))
                .thenThrow(new UserAlreadyExistsException("Ya existe un usuario con ese username o email"));

        mockMvc.perform(post("/integrador/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("9994"));
    }

    @Test
    void registroValidoLlegaAlServicio() throws Exception {
        when(proyectoService.registerUser(any())).thenReturn(new ResponseRegister());

        mockMvc.perform(post("/integrador/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"));
    }
}
