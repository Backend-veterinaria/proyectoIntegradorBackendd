package com.example.proyectoIntegrador.controllers;

import com.example.proyectoIntegrador.controllers.Impl.ProyectoControllerImpl;
import com.example.proyectoIntegrador.exceptions.UserAlreadyExistsException;
import com.example.proyectoIntegrador.models.ResponseRegister;
import com.example.proyectoIntegrador.services.ProyectoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProyectoControllerImpl.class)
class ProyectoControllerImplTest {

    private static final String VALID_REGISTER = """
            {"nombre":"Ana","apellido":"Gomez","direccion":"Calle 1","telefono":"3001234567",
             "email":"ana@correo.com","username":"ana","password":"secreta123","rolUser":1}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProyectoService proyectoService;

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
                .andExpect(jsonPath("$.responseObj", hasItem("password: password must have between 8 and 72 characters")))
                .andExpect(jsonPath("$.responseObj", hasItem("rolUser: you must specify the rolUser field")));

        verify(proyectoService, never()).registerUser(any());
    }

    @Test
    void jsonMalFormadoDevuelveErrorDeValidacion() throws Exception {
        mockMvc.perform(post("/integrador/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rolUser\":\"abc\"}"))
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
