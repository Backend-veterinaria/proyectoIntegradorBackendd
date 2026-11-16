package com.example.proyectoIntegrador.controllers;

import com.example.proyectoIntegrador.controllers.Impl.ClientControllerImpl;
import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.ClientInfo;
import com.example.proyectoIntegrador.security.AuthenticatedUser;
import com.example.proyectoIntegrador.security.JwtService;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.security.SecurityConfig;
import com.example.proyectoIntegrador.services.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientControllerImpl.class)
@Import({SecurityConfig.class, JwtService.class})
class ClientControllerImplTest {

    private static final String UPDATE_BODY = """
            {"nombre":"Ana","apellido":"Gomez","direccion":"Calle 1","telefono":"3109999999","email":"ana@correo.com"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ClientService clientService;

    private String bearer(int userId, Role role) {
        return "Bearer " + jwtService.generateToken(userId, "usuario", role);
    }

    private static ClientInfo client(int id) {
        ClientInfo info = new ClientInfo();
        info.setClienteId(id);
        info.setTelefono("3109999999");
        return info;
    }

    @Test
    void clienteConsultaSuPerfil() throws Exception {
        when(clientService.get(any(), eq(5))).thenReturn(client(5));

        mockMvc.perform(get("/integrador/clients/5").header(HttpHeaders.AUTHORIZATION, bearer(5, Role.CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseObj.clienteId").value(5));

        verify(clientService).get(new AuthenticatedUser(5, "usuario", Role.CLIENTE), 5);
    }

    @Test
    void consultarOtroPerfilDevuelve403() throws Exception {
        when(clientService.get(any(), anyInt())).thenThrow(new AccessDeniedException("Solo puede acceder a sus propios datos"));

        mockMvc.perform(get("/integrador/clients/6").header(HttpHeaders.AUTHORIZATION, bearer(5, Role.CLIENTE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.responseCode").value("6619"));
    }

    @Test
    void clienteEditaSuTelefono() throws Exception {
        when(clientService.update(any(), eq(5), any())).thenReturn(client(5));

        mockMvc.perform(put("/integrador/clients/5")
                        .header(HttpHeaders.AUTHORIZATION, bearer(5, Role.CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.responseObj.telefono").value("3109999999"));
    }

    @Test
    void editarConEmailInvalidoDevuelveErrorDeValidacion() throws Exception {
        mockMvc.perform(put("/integrador/clients/5")
                        .header(HttpHeaders.AUTHORIZATION, bearer(5, Role.CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"apellido\":\"Gomez\",\"direccion\":\"Calle 1\",\"telefono\":\"300\",\"email\":\"no-es-correo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6617"))
                .andExpect(jsonPath("$.responseObj", hasItem("email: email is not valid")));

        verify(clientService, never()).update(any(), anyInt(), any());
    }

    @Test
    void errorDeNegocioAlEditarSeMuestra() throws Exception {
        when(clientService.update(any(), anyInt(), any()))
                .thenThrow(new BusinessException("El email ya esta registrado por otro usuario"));

        mockMvc.perform(put("/integrador/clients/5")
                        .header(HttpHeaders.AUTHORIZATION, bearer(5, Role.CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6616"))
                .andExpect(jsonPath("$.responseObj").value("El email ya esta registrado por otro usuario"));
    }

    @Test
    void personalListaPropietarios() throws Exception {
        when(clientService.list(false, null)).thenReturn(List.of(client(5)));

        mockMvc.perform(get("/integrador/clients").header(HttpHeaders.AUTHORIZATION, bearer(40, Role.VETERINARIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseObj[0].clienteId").value(5));

        verify(clientService).list(eq(false), isNull());
    }

    @Test
    void personalBuscaPropietarios() throws Exception {
        when(clientService.list(true, "gomez")).thenReturn(List.of());

        mockMvc.perform(get("/integrador/clients").param("includeInactive", "true").param("q", "gomez")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1, Role.ADMIN)))
                .andExpect(status().isOk());

        verify(clientService).list(true, "gomez");
    }

    @Test
    void clienteNoPuedeListarPropietarios() throws Exception {
        mockMvc.perform(get("/integrador/clients").header(HttpHeaders.AUTHORIZATION, bearer(5, Role.CLIENTE)))
                .andExpect(status().isForbidden());

        verify(clientService, never()).list(anyBoolean(), any());
    }

    @Test
    void adminInactivaPropietario() throws Exception {
        ClientInfo inactive = client(6);
        inactive.setActivo(false);
        when(clientService.changeStatus(any(), eq(6), eq(false))).thenReturn(inactive);

        mockMvc.perform(patch("/integrador/clients/6/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseObj.activo").value(false));
    }

    @Test
    void veterinarioNoPuedeInactivarPropietarios() throws Exception {
        mockMvc.perform(patch("/integrador/clients/6/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(40, Role.VETERINARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isForbidden());

        verify(clientService, never()).changeStatus(any(), anyInt(), anyBoolean());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/integrador/clients/5"))
                .andExpect(status().isUnauthorized());
    }
}
