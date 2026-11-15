package com.example.proyectoIntegrador.controllers;

import com.example.proyectoIntegrador.controllers.Impl.VeterinarianControllerImpl;
import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.exceptions.UserAlreadyExistsException;
import com.example.proyectoIntegrador.models.VeterinarianInfo;
import com.example.proyectoIntegrador.security.JwtService;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.security.SecurityConfig;
import com.example.proyectoIntegrador.services.VeterinarianService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VeterinarianControllerImpl.class)
@Import({SecurityConfig.class, JwtService.class})
class VeterinarianControllerImplTest {

    private static final String URL = "/integrador/veterinarians";
    private static final String VALID_CREATE = """
            {"nombre":"Laura","apellido":"Perez","telefono":"3001234567","email":"laura@vet.com",
             "username":"dr.perez","password":"secreta123"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private VeterinarianService veterinarianService;

    private String bearer(Role role) {
        return "Bearer " + jwtService.generateToken(1, "usuario", role);
    }

    private static VeterinarianInfo vet() {
        VeterinarianInfo info = new VeterinarianInfo();
        info.setMedicoId(11);
        info.setNombre("Laura");
        info.setActivo(true);
        return info;
    }

    @Test
    void adminCreaVeterinario() throws Exception {
        when(veterinarianService.create(any())).thenReturn(vet());

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.responseObj.medicoId").value(11));
    }

    @Test
    void clienteNoPuedeCrearVeterinarios() throws Exception {
        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.responseCode").value("6619"));

        verify(veterinarianService, never()).create(any());
    }

    @Test
    void veterinarioNoPuedeGestionarVeterinarios() throws Exception {
        mockMvc.perform(patch(URL + "/11/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.VETERINARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isForbidden());

        verify(veterinarianService, never()).changeStatus(anyInt(), anyBoolean());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearConDatosInvalidosDevuelveErrorDeValidacion() throws Exception {
        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Laura\",\"email\":\"no-es-correo\",\"password\":\"corta\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6617"))
                .andExpect(jsonPath("$.responseObj", hasItem("email: email is not valid")));

        verify(veterinarianService, never()).create(any());
    }

    @Test
    void crearDuplicadoDevuelveUserExists() throws Exception {
        when(veterinarianService.create(any()))
                .thenThrow(new UserAlreadyExistsException("Ya existe un usuario con ese username o email"));

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("9994"));
    }

    @Test
    void listarPorDefectoSoloActivos() throws Exception {
        when(veterinarianService.list(false)).thenReturn(List.of(vet()));

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseObj[0].medicoId").value(11));

        verify(veterinarianService).list(false);
    }

    @Test
    void listarIncluyendoInactivos() throws Exception {
        when(veterinarianService.list(true)).thenReturn(List.of());

        mockMvc.perform(get(URL).param("includeInactive", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isOk());

        verify(veterinarianService).list(true);
    }

    @Test
    void consultarInexistenteDevuelveMensajeDeNegocio() throws Exception {
        when(veterinarianService.get(99)).thenThrow(new BusinessException("El veterinario no existe"));

        mockMvc.perform(get(URL + "/99").header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6615"))
                .andExpect(jsonPath("$.responseObj").value("El veterinario no existe"));
    }

    @Test
    void editarVeterinario() throws Exception {
        when(veterinarianService.update(eqInt(11), any())).thenReturn(vet());

        mockMvc.perform(put(URL + "/11")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Laura\",\"apellido\":\"Gomez\",\"telefono\":\"300\",\"email\":\"laura@vet.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"));
    }

    @Test
    void inactivarVeterinario() throws Exception {
        VeterinarianInfo inactive = vet();
        inactive.setActivo(false);
        when(veterinarianService.changeStatus(11, false)).thenReturn(inactive);

        mockMvc.perform(patch(URL + "/11/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseObj.activo").value(false));
    }

    @Test
    void cambiarEstadoSinCampoActivoDevuelveErrorDeValidacion() throws Exception {
        mockMvc.perform(patch(URL + "/11/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6617"));
    }

    private static int eqInt(int value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
