package com.example.proyectoIntegrador.controllers;

import com.example.proyectoIntegrador.controllers.Impl.ReportControllerImpl;
import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.Report;
import com.example.proyectoIntegrador.models.ReportRows;
import com.example.proyectoIntegrador.security.JwtService;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.security.SecurityConfig;
import com.example.proyectoIntegrador.services.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportControllerImpl.class)
@Import({SecurityConfig.class, JwtService.class})
class ReportControllerImplTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ReportService reportService;

    private String bearer(int userId, Role role) {
        return "Bearer " + jwtService.generateToken(userId, "usuario", role);
    }

    @Test
    void adminConsultaCitasPorEstado() throws Exception {
        when(reportService.appointmentsByStatus(any(), any())).thenReturn(new Report<>("2026-11-01", "2026-11-30", null, 4,
                List.of(new ReportRows.AppointmentsByStatus(1, "Programada", "PROGRAMADA", 4))));

        mockMvc.perform(get("/integrador/reports/appointmentsByStatus")
                        .param("desde", "20261101").param("hasta", "20261130")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.responseObj.total").value(4))
                .andExpect(jsonPath("$.responseObj.items[0].status").value("PROGRAMADA"))
                .andExpect(jsonPath("$.responseObj.items[0].cantidad").value(4));
    }

    @Test
    void veterinarioConsultaMascotasAtendidas() throws Exception {
        when(reportService.attendedPets(any(), any())).thenReturn(new Report<>("2026-11-01", "2026-11-30", 11, 1,
                List.of(new ReportRows.AttendedPet(7, "Firulais", "Labrador", 5, "Ana Gomez", 2, "2026-11-20T14:42:10"))));

        mockMvc.perform(get("/integrador/reports/attendedPets")
                        .param("desde", "20261101").param("hasta", "20261130")
                        .header(HttpHeaders.AUTHORIZATION, bearer(40, Role.VETERINARIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseObj.items[0].mascotaNombre").value("Firulais"));
    }

    @Test
    void sinFechasEsErrorDeValidacion() throws Exception {
        mockMvc.perform(get("/integrador/reports/attentionsByVeterinarian")
                        .param("desde", "2026-11-01")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1, Role.ADMIN)))
                .andExpect(jsonPath("$.responseCode").value("6617"))
                .andExpect(jsonPath("$.responseObj", hasItem("hasta: required field hasta")))
                .andExpect(jsonPath("$.responseObj", hasItem("desde: desde must have format yyyyMMdd")));

        verify(reportService, never()).attentionsByVeterinarian(any(), any());
    }

    @Test
    void errorDeNegocioSeMuestra() throws Exception {
        when(reportService.appointmentsByStatus(any(), any()))
                .thenThrow(new BusinessException("La fecha hasta no puede ser anterior a la fecha desde"));

        mockMvc.perform(get("/integrador/reports/appointmentsByStatus")
                        .param("desde", "20261130").param("hasta", "20261101")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1, Role.ADMIN)))
                .andExpect(jsonPath("$.responseCode").value("6615"))
                .andExpect(jsonPath("$.responseObj").value("La fecha hasta no puede ser anterior a la fecha desde"));
    }

    @Test
    void veterinarioPidiendoDatosDeOtroDevuelve403() throws Exception {
        when(reportService.appointmentsByStatus(any(), any()))
                .thenThrow(new AccessDeniedException("Un veterinario solo puede consultar sus propios reportes"));

        mockMvc.perform(get("/integrador/reports/appointmentsByStatus")
                        .param("desde", "20261101").param("hasta", "20261130").param("medicoId", "12")
                        .header(HttpHeaders.AUTHORIZATION, bearer(40, Role.VETERINARIO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.responseCode").value("6619"));
    }

    @Test
    void clienteNoVeReportes() throws Exception {
        mockMvc.perform(get("/integrador/reports/appointmentsByStatus")
                        .param("desde", "20261101").param("hasta", "20261130")
                        .header(HttpHeaders.AUTHORIZATION, bearer(5, Role.CLIENTE)))
                .andExpect(status().isForbidden());

        verify(reportService, never()).appointmentsByStatus(any(), any());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/integrador/reports/appointmentsByStatus")
                        .param("desde", "20261101").param("hasta", "20261130"))
                .andExpect(status().isUnauthorized());
    }
}
