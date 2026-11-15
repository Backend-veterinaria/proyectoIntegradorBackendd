package com.example.proyectoIntegrador.controllers;

import com.example.proyectoIntegrador.controllers.Impl.AgendaControllerImpl;
import com.example.proyectoIntegrador.services.AgendaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgendaControllerImpl.class)
class AgendaControllerImplTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AgendaService agendaService;

    @Test
    void listarCitasSinIdClientDevuelveErrorDeValidacion() throws Exception {
        mockMvc.perform(get("/integrador/agendaCita/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6617"))
                .andExpect(jsonPath("$.responseObj", hasItem("idClient: required field idClient")));

        verify(agendaService, never()).getListCitas(anyInt());
    }

    @Test
    void listarCitasConIdClientValido() throws Exception {
        when(agendaService.getListCitas(3)).thenReturn(List.of());

        mockMvc.perform(get("/integrador/agendaCita/list").param("idClient", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"));
    }

    @Test
    void agendarCitaConFechaMalFormadaDevuelveErrorDeValidacion() throws Exception {
        mockMvc.perform(post("/integrador/agendaCita/scheduleAnAppointment")
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
