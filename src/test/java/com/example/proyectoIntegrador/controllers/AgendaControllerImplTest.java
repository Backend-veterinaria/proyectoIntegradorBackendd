package com.example.proyectoIntegrador.controllers;

import com.example.proyectoIntegrador.controllers.Impl.AgendaControllerImpl;
import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.exceptions.ErrorMessages;
import com.example.proyectoIntegrador.models.VeterinarianAgendaItem;
import com.example.proyectoIntegrador.security.AuthenticatedUser;
import org.springframework.security.access.AccessDeniedException;
import static org.mockito.ArgumentMatchers.eq;
import com.example.proyectoIntegrador.security.JwtService;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.security.SecurityConfig;
import com.example.proyectoIntegrador.services.AgendaService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgendaControllerImpl.class)
@Import({SecurityConfig.class, JwtService.class})
class AgendaControllerImplTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private AgendaService agendaService;

    @MockBean
    private com.example.proyectoIntegrador.services.ScheduleService scheduleService;

    private String bearer(int userId, String username) {
        return bearer(userId, username, Role.CLIENTE);
    }

    private String bearer(int userId, String username, Role role) {
        return "Bearer " + jwtService.generateToken(userId, username, role);
    }

    @Test
    void veterinarioPuedeListarCitasDeCualquierCliente() throws Exception {
        when(agendaService.getListCitas(4)).thenReturn(List.of());

        mockMvc.perform(get("/integrador/agendaCita/list").param("idClient", "4")
                        .header(HttpHeaders.AUTHORIZATION, bearer(9, "dr.perez", Role.VETERINARIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"));
    }

    @Test
    void adminPuedeCancelarCitaDeOtroCliente() throws Exception {
        when(agendaService.cancelledAppointment(any())).thenReturn(true);

        mockMvc.perform(patch("/integrador/agendaCita/cancelledAppointment")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1, "admin", Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCliente\":4,\"idAppointment\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"));
    }

    private static final String VET_AGENDA_URL = "/integrador/agendaCita/veterinarian";

    @Test
    void veterinarioConsultaSuAgenda() throws Exception {
        VeterinarianAgendaItem item = new VeterinarianAgendaItem();
        item.setAgendaId(100);
        item.setHora("10:00:00");
        when(agendaService.getVeterinarianAgenda(any(), any())).thenReturn(List.of(item));

        mockMvc.perform(get(VET_AGENDA_URL).param("fecha", "20261120")
                        .header(HttpHeaders.AUTHORIZATION, bearer(40, "dr.perez", Role.VETERINARIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.responseObj[0].agendaId").value(100));

        // El servicio recibe el usuario del token
        verify(agendaService).getVeterinarianAgenda(eq(new AuthenticatedUser(40, "dr.perez", Role.VETERINARIO)), any());
    }

    @Test
    void clienteNoPuedeConsultarAgendaDeVeterinario() throws Exception {
        mockMvc.perform(get(VET_AGENDA_URL).param("fecha", "20261120").param("medicoId", "11")
                        .header(HttpHeaders.AUTHORIZATION, bearer(3, "ana")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.responseCode").value("6619"));

        verify(agendaService, never()).getVeterinarianAgenda(any(), any());
    }

    @Test
    void veterinarioConsultandoAgendaDeOtroDevuelve403() throws Exception {
        when(agendaService.getVeterinarianAgenda(any(), any()))
                .thenThrow(new AccessDeniedException("Un veterinario solo puede consultar su propia agenda"));

        mockMvc.perform(get(VET_AGENDA_URL).param("fecha", "20261120").param("medicoId", "12")
                        .header(HttpHeaders.AUTHORIZATION, bearer(40, "dr.perez", Role.VETERINARIO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.responseCode").value("6619"));
    }

    @Test
    void agendaConFechaMalFormadaDevuelveErrorDeValidacion() throws Exception {
        mockMvc.perform(get(VET_AGENDA_URL).param("fecha", "2026-11-20")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1, "admin", Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6617"))
                .andExpect(jsonPath("$.responseObj", hasItem("fecha: fecha must have format yyyyMMdd")));

        verify(agendaService, never()).getVeterinarianAgenda(any(), any());
    }

    @Test
    void errorDeNegocioEnAgendaSeMuestraAlUsuario() throws Exception {
        when(agendaService.getVeterinarianAgenda(any(), any()))
                .thenThrow(new BusinessException("Debe indicar el veterinario (medicoId)"));

        mockMvc.perform(get(VET_AGENDA_URL).param("fecha", "20261120")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1, "admin", Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6615"))
                .andExpect(jsonPath("$.responseObj").value("Debe indicar el veterinario (medicoId)"));
    }

    // ---------- VET-03: cambio de estado ----------

    @Test
    void veterinarioCambiaEstadoDeCita() throws Exception {
        VeterinarianAgendaItem cita = new VeterinarianAgendaItem();
        cita.setAgendaId(100);
        cita.setEstado("Atendida");
        when(agendaService.changeAppointmentStatus(any(), eq(100), eq(5))).thenReturn(cita);

        mockMvc.perform(patch("/integrador/agendaCita/100/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(40, "dr.perez", Role.VETERINARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.responseObj.estado").value("Atendida"));
    }

    @Test
    void clienteNoPuedeCambiarEstadoDeCita() throws Exception {
        mockMvc.perform(patch("/integrador/agendaCita/100/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(3, "ana"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":5}"))
                .andExpect(status().isForbidden());

        verify(agendaService, never()).changeAppointmentStatus(any(), anyInt(), anyInt());
    }

    @Test
    void veterinarioCambiandoCitaDeOtroDevuelve403() throws Exception {
        when(agendaService.changeAppointmentStatus(any(), anyInt(), anyInt()))
                .thenThrow(new AccessDeniedException("Un veterinario solo puede gestionar sus propias citas"));

        mockMvc.perform(patch("/integrador/agendaCita/100/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(40, "dr.perez", Role.VETERINARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":5}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.responseCode").value("6619"));
    }

    @Test
    void transicionNoPermitidaDevuelveMensajeDeNegocio() throws Exception {
        when(agendaService.changeAppointmentStatus(any(), anyInt(), anyInt()))
                .thenThrow(new BusinessException("No se puede cambiar una cita en estado 'Cancelada' a 'Atendida'"));

        mockMvc.perform(patch("/integrador/agendaCita/100/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1, "admin", Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoId\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6616"))
                .andExpect(jsonPath("$.responseObj").value("No se puede cambiar una cita en estado 'Cancelada' a 'Atendida'"));
    }

    @Test
    void cambioDeEstadoSinEstadoIdDevuelveErrorDeValidacion() throws Exception {
        mockMvc.perform(patch("/integrador/agendaCita/100/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1, "admin", Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6617"));
    }

    // ---------- VET-04: reprogramación ----------

    @Test
    void clienteReprogramaCita() throws Exception {
        VeterinarianAgendaItem cita = new VeterinarianAgendaItem();
        cita.setAgendaId(100);
        cita.setEstado("Reprogramada");
        when(agendaService.rescheduleAppointment(any(), eq(100), any())).thenReturn(cita);

        mockMvc.perform(patch("/integrador/agendaCita/100/reschedule")
                        .header(HttpHeaders.AUTHORIZATION, bearer(3, "ana"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"20991120\",\"hora\":\"103000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.responseObj.estado").value("Reprogramada"));

        verify(agendaService).rescheduleAppointment(eq(new AuthenticatedUser(3, "ana", Role.CLIENTE)), eq(100), any());
    }

    @Test
    void reprogramarCitaAjenaDevuelve403() throws Exception {
        when(agendaService.rescheduleAppointment(any(), anyInt(), any()))
                .thenThrow(new AccessDeniedException("Solo puede reprogramar sus propias citas"));

        mockMvc.perform(patch("/integrador/agendaCita/100/reschedule")
                        .header(HttpHeaders.AUTHORIZATION, bearer(3, "ana"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"20991120\",\"hora\":\"103000\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.responseCode").value("6619"));
    }

    @Test
    void reprogramarSinTokenDevuelve401() throws Exception {
        mockMvc.perform(patch("/integrador/agendaCita/100/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"20991120\",\"hora\":\"103000\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reprogramarConHoraMalFormadaDevuelveErrorDeValidacion() throws Exception {
        mockMvc.perform(patch("/integrador/agendaCita/100/reschedule")
                        .header(HttpHeaders.AUTHORIZATION, bearer(3, "ana"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"20991120\",\"hora\":\"10:30\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6617"))
                .andExpect(jsonPath("$.responseObj", hasItem("hora: hora must have format HHmmss")));

        verify(agendaService, never()).rescheduleAppointment(any(), anyInt(), any());
    }

    @Test
    void errorInternoNoSeEnviaAlCliente() throws Exception {
        when(agendaService.getListCitas(3))
                .thenThrow(new RuntimeException("ERROR: relation \"agenda_cita\" does not exist"));

        mockMvc.perform(get("/integrador/agendaCita/list").param("idClient", "3")
                        .header(HttpHeaders.AUTHORIZATION, bearer(3, "ana")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6615"))
                .andExpect(jsonPath("$.responseObj").value(ErrorMessages.GENERIC_MESSAGE));
    }

    @Test
    void errorDeNegocioSiSeEnviaAlCliente() throws Exception {
        when(agendaService.getListCitas(3)).thenThrow(new BusinessException("El usuario no tiene citas agendadas"));

        mockMvc.perform(get("/integrador/agendaCita/list").param("idClient", "3")
                        .header(HttpHeaders.AUTHORIZATION, bearer(3, "ana")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6615"))
                .andExpect(jsonPath("$.responseObj").value("El usuario no tiene citas agendadas"));
    }

    @Test
    void rutaInexistenteDevuelve404ConElFormatoComun() throws Exception {
        mockMvc.perform(get("/integrador/agendaCita/no-existe")
                        .header(HttpHeaders.AUTHORIZATION, bearer(3, "ana")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.responseCode").value("6620"))
                .andExpect(jsonPath("$.responseObj").value(ErrorMessages.GENERIC_MESSAGE));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/integrador/agendaCita/list").param("idClient", "3"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.responseCode").value("6618"));

        verify(agendaService, never()).getListCitas(anyInt());
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        mockMvc.perform(get("/integrador/agendaCita/list").param("idClient", "3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token.falso.123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.responseCode").value("6618"));
    }

    @Test
    void listarCitasConTokenDelMismoCliente() throws Exception {
        when(agendaService.getListCitas(3)).thenReturn(List.of());

        mockMvc.perform(get("/integrador/agendaCita/list").param("idClient", "3")
                        .header(HttpHeaders.AUTHORIZATION, bearer(3, "ana")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"));
    }

    @Test
    void listarCitasDeOtroClienteDevuelve403() throws Exception {
        mockMvc.perform(get("/integrador/agendaCita/list").param("idClient", "4")
                        .header(HttpHeaders.AUTHORIZATION, bearer(3, "ana")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.responseCode").value("6619"));

        verify(agendaService, never()).getListCitas(anyInt());
    }

    @Test
    void cancelarCitaDeOtroClienteDevuelve403() throws Exception {
        mockMvc.perform(patch("/integrador/agendaCita/cancelledAppointment")
                        .header(HttpHeaders.AUTHORIZATION, bearer(3, "ana"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCliente\":4,\"idAppointment\":10}"))
                .andExpect(status().isForbidden());

        verify(agendaService, never()).cancelledAppointment(any());
    }

    @Test
    void detalleConUsernameDeOtroUsuarioDevuelve403() throws Exception {
        mockMvc.perform(get("/integrador/agendaCita/AnAppointment/details")
                        .param("idAppointment", "10").param("idClient", "3").param("username", "otro")
                        .header(HttpHeaders.AUTHORIZATION, bearer(3, "ana")))
                .andExpect(status().isForbidden());

        verify(agendaService, never()).getDetailAppointment(any());
    }

    @Test
    void listarCitasSinIdClientDevuelveErrorDeValidacion() throws Exception {
        mockMvc.perform(get("/integrador/agendaCita/list")
                        .header(HttpHeaders.AUTHORIZATION, bearer(3, "ana")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6617"))
                .andExpect(jsonPath("$.responseObj", hasItem("idClient: required field idClient")));

        verify(agendaService, never()).getListCitas(anyInt());
    }

    @Test
    void agendarCitaConFechaMalFormadaDevuelveErrorDeValidacion() throws Exception {
        mockMvc.perform(post("/integrador/agendaCita/scheduleAnAppointment")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1, "ana"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estadoId":1,"clienteId":1,"medicoId":1,"mascotaId":1,
                                 "description":"Control","fecha":"2026-11-20","hora":"100000"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6617"))
                .andExpect(jsonPath("$.responseObj", hasItem("fecha: fecha must have format yyyyMMdd")));
    }
}
