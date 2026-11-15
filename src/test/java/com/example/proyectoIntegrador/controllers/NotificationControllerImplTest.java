package com.example.proyectoIntegrador.controllers;

import com.example.proyectoIntegrador.controllers.Impl.NotificationControllerImpl;
import com.example.proyectoIntegrador.security.JwtService;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.security.SecurityConfig;
import com.example.proyectoIntegrador.services.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationControllerImpl.class)
@Import({SecurityConfig.class, JwtService.class})
class NotificationControllerImplTest {

    private static final String SSE_URL = "/integrador/notifications/connection";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private NotificationService notificationService;

    @Test
    void conexionSseSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get(SSE_URL).param("client_id", "3"))
                .andExpect(status().isUnauthorized());

        verify(notificationService, never()).stablishConnection(anyString());
    }

    @Test
    void conexionSseConTokenEnParametro() throws Exception {
        when(notificationService.stablishConnection("3")).thenReturn(new SseEmitter());

        mockMvc.perform(get(SSE_URL).param("token", jwtService.generateToken(3, "ana", Role.CLIENTE)))
                .andExpect(request().asyncStarted());

        verify(notificationService).stablishConnection("3");
    }

    @Test
    void conexionSseDeOtroClienteDevuelve403() throws Exception {
        mockMvc.perform(get(SSE_URL)
                        .param("client_id", "4")
                        .param("token", jwtService.generateToken(3, "ana", Role.CLIENTE)))
                .andExpect(status().isForbidden());

        verify(notificationService, never()).stablishConnection(anyString());
    }

    @Test
    void niElAdminPuedeAbrirElCanalDeOtroCliente() throws Exception {
        mockMvc.perform(get(SSE_URL)
                        .param("client_id", "4")
                        .param("token", jwtService.generateToken(1, "admin", Role.ADMIN)))
                .andExpect(status().isForbidden());

        verify(notificationService, never()).stablishConnection(anyString());
    }
}
