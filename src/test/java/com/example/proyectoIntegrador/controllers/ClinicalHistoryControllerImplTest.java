package com.example.proyectoIntegrador.controllers;

import com.example.proyectoIntegrador.controllers.Impl.ClinicalHistoryControllerImpl;
import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.ClinicalAttention;
import com.example.proyectoIntegrador.models.ClinicalHistory;
import com.example.proyectoIntegrador.models.InfoMascota;
import com.example.proyectoIntegrador.security.AuthenticatedUser;
import com.example.proyectoIntegrador.security.JwtService;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.security.SecurityConfig;
import com.example.proyectoIntegrador.services.ClinicalHistoryService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClinicalHistoryControllerImpl.class)
@Import({SecurityConfig.class, JwtService.class})
class ClinicalHistoryControllerImplTest {

    private static final String BODY = """
            {"agendaId":100,"motivoConsulta":"Rasca el oido","diagnostico":"Otitis externa",
             "procedimientos":"Limpieza","pesoKg":12.5,"temperaturaC":38.6}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ClinicalHistoryService clinicalHistoryService;

    private String bearer(int userId, Role role) {
        return "Bearer " + jwtService.generateToken(userId, "usuario", role);
    }

    private static ClinicalAttention saved() {
        ClinicalAttention a = new ClinicalAttention();
        a.setAtencionId(500);
        a.setAgendaId(100);
        a.setDiagnostico("Otitis externa");
        return a;
    }

    @Test
    void veterinarioRegistraAtencion() throws Exception {
        when(clinicalHistoryService.registerAttention(any(), any())).thenReturn(saved());

        mockMvc.perform(post("/integrador/clinicalHistory/attentions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(40, Role.VETERINARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.responseObj.atencionId").value(500));

        verify(clinicalHistoryService).registerAttention(eq(new AuthenticatedUser(40, "usuario", Role.VETERINARIO)), any());
    }

    @Test
    void sinDiagnosticoEsErrorDeValidacion() throws Exception {
        mockMvc.perform(post("/integrador/clinicalHistory/attentions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(40, Role.VETERINARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agendaId\":100,\"diagnostico\":\" \",\"temperaturaC\":50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6617"))
                .andExpect(jsonPath("$.responseObj", hasItem("diagnostico: required field diagnostico")))
                .andExpect(jsonPath("$.responseObj", hasItem("temperaturaC: temperaturaC must be between 30 and 45")));

        verify(clinicalHistoryService, never()).registerAttention(any(), any());
    }

    @Test
    void errorDeNegocioSeMuestra() throws Exception {
        when(clinicalHistoryService.registerAttention(any(), any()))
                .thenThrow(new BusinessException("La cita ya tiene una atencion registrada"));

        mockMvc.perform(post("/integrador/clinicalHistory/attentions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(40, Role.VETERINARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6621"))
                .andExpect(jsonPath("$.responseObj").value("La cita ya tiene una atencion registrada"));
    }

    @Test
    void citaDeOtroVeterinarioDevuelve403() throws Exception {
        when(clinicalHistoryService.registerAttention(any(), any()))
                .thenThrow(new AccessDeniedException("Solo el veterinario asignado puede registrar la atencion de la cita"));

        mockMvc.perform(post("/integrador/clinicalHistory/attentions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(40, Role.VETERINARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.responseCode").value("6619"));
    }

    @Test
    void adminYClienteNoRegistranAtenciones() throws Exception {
        for (Role role : new Role[]{Role.ADMIN, Role.CLIENTE}) {
            mockMvc.perform(post("/integrador/clinicalHistory/attentions")
                            .header(HttpHeaders.AUTHORIZATION, bearer(1, role))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(BODY))
                    .andExpect(status().isForbidden());
        }
        verify(clinicalHistoryService, never()).registerAttention(any(), any());
    }

    @Test
    void adminConsultaAtencion() throws Exception {
        when(clinicalHistoryService.getAttention(any(), eq(500))).thenReturn(saved());

        mockMvc.perform(get("/integrador/clinicalHistory/attentions/500")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseObj.diagnostico").value("Otitis externa"));
    }

    @Test
    void clienteNoConsultaAtencionAjena() throws Exception {
        when(clinicalHistoryService.getAttention(any(), anyInt()))
                .thenThrow(new AccessDeniedException("Solo puede consultar la historia clinica de sus mascotas"));

        mockMvc.perform(get("/integrador/clinicalHistory/attentions/500")
                        .header(HttpHeaders.AUTHORIZATION, bearer(5, Role.CLIENTE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.responseCode").value("6619"));
    }

    @Test
    void duenoConsultaHistoriaDeSuMascota() throws Exception {
        InfoMascota mascota = new InfoMascota();
        mascota.setIdMascota(7);
        when(clinicalHistoryService.getPetHistory(any(), eq(7)))
                .thenReturn(new ClinicalHistory(mascota, List.of(saved()), List.of(), List.of()));

        mockMvc.perform(get("/integrador/clinicalHistory/pets/7")
                        .header(HttpHeaders.AUTHORIZATION, bearer(5, Role.CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseObj.mascota.idMascota").value(7))
                .andExpect(jsonPath("$.responseObj.atenciones[0].atencionId").value(500));

        verify(clinicalHistoryService).getPetHistory(new AuthenticatedUser(5, "usuario", Role.CLIENTE), 7);
    }

    @Test
    void historiaDeMascotaAjenaDevuelve403() throws Exception {
        when(clinicalHistoryService.getPetHistory(any(), anyInt()))
                .thenThrow(new AccessDeniedException("Solo puede consultar la historia clinica de sus mascotas"));

        mockMvc.perform(get("/integrador/clinicalHistory/pets/8")
                        .header(HttpHeaders.AUTHORIZATION, bearer(5, Role.CLIENTE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void historiaDeMascotaInexistenteEsError() throws Exception {
        when(clinicalHistoryService.getPetHistory(any(), anyInt())).thenThrow(new BusinessException("La mascota no existe"));

        mockMvc.perform(get("/integrador/clinicalHistory/pets/99")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6615"))
                .andExpect(jsonPath("$.responseObj").value("La mascota no existe"));
    }

    @Test
    void veterinarioRegistraTratamiento() throws Exception {
        com.example.proyectoIntegrador.models.Treatment t = new com.example.proyectoIntegrador.models.Treatment();
        t.setTratamientoId(300);
        t.setEstado("EN_CURSO");
        when(clinicalHistoryService.registerTreatment(any(), any())).thenReturn(t);

        mockMvc.perform(post("/integrador/clinicalHistory/treatments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(40, Role.VETERINARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mascotaId\":7,\"medicamento\":\"Amoxicilina\",\"dosis\":\"250 mg\",\"fechaInicio\":\"20261120\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseObj.tratamientoId").value(300));
    }

    @Test
    void tratamientoSinDosisEsErrorDeValidacion() throws Exception {
        mockMvc.perform(post("/integrador/clinicalHistory/treatments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(40, Role.VETERINARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mascotaId\":7,\"medicamento\":\"Amoxicilina\",\"fechaInicio\":\"2026-11-20\"}"))
                .andExpect(jsonPath("$.responseCode").value("6617"))
                .andExpect(jsonPath("$.responseObj", hasItem("dosis: required field dosis")))
                .andExpect(jsonPath("$.responseObj", hasItem("fechaInicio: fechaInicio must have format yyyyMMdd")));

        verify(clinicalHistoryService, never()).registerTreatment(any(), any());
    }

    @Test
    void veterinarioFinalizaTratamiento() throws Exception {
        when(clinicalHistoryService.changeTreatmentStatus(any(), eq(300),
                eq(com.example.proyectoIntegrador.enums.TreatmentStatus.FINALIZADO)))
                .thenReturn(new com.example.proyectoIntegrador.models.Treatment());

        mockMvc.perform(patch("/integrador/clinicalHistory/treatments/300/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(40, Role.VETERINARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"FINALIZADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"));
    }

    @Test
    void estadoDeTratamientoInvalidoEsErrorDeValidacion() throws Exception {
        mockMvc.perform(patch("/integrador/clinicalHistory/treatments/300/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(40, Role.VETERINARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_CURSO\"}"))
                .andExpect(jsonPath("$.responseCode").value("6617"));
    }

    @Test
    void clienteNoRegistraVacunas() throws Exception {
        mockMvc.perform(post("/integrador/clinicalHistory/vaccines")
                        .header(HttpHeaders.AUTHORIZATION, bearer(5, Role.CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mascotaId\":7,\"nombre\":\"Rabia\",\"fechaAplicacion\":\"20261115\"}"))
                .andExpect(status().isForbidden());

        verify(clinicalHistoryService, never()).registerVaccine(any(), any());
    }

    @Test
    void clienteConsultaVacunasPendientes() throws Exception {
        when(clinicalHistoryService.getUpcomingVaccines(any(), eq(30), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/integrador/clinicalHistory/vaccines/upcoming")
                        .header(HttpHeaders.AUTHORIZATION, bearer(5, Role.CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/integrador/clinicalHistory/attentions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }
}
