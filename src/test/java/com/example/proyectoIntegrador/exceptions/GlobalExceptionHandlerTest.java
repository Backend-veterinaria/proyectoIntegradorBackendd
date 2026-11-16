package com.example.proyectoIntegrador.exceptions;

import com.example.proyectoIntegrador.controllers.Impl.ClinicalHistoryControllerImpl;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA-01: todos los errores salen con la estructura ResponseGeneric y sin detalles internos (RNF-05).
 */
@WebMvcTest(ClinicalHistoryControllerImpl.class)
@Import({SecurityConfig.class, JwtService.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ClinicalHistoryService clinicalHistoryService;

    private String token() {
        return "Bearer " + jwtService.generateToken(1, "admin", Role.ADMIN);
    }

    @Test
    void parametroDeRutaConTipoInvalidoEsErrorDeValidacion() throws Exception {
        mockMvc.perform(get("/integrador/clinicalHistory/pets/abc").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6617"))
                .andExpect(jsonPath("$.responseObj", hasItem("mascotaId: invalid value")));
    }

    @Test
    void parametroDeUrlConTipoInvalidoEsErrorDeValidacion() throws Exception {
        mockMvc.perform(get("/integrador/clinicalHistory/vaccines/upcoming").param("days", "diez")
                        .header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6617"))
                .andExpect(jsonPath("$.responseObj", hasItem("days: invalid value")));
    }

    @Test
    void jsonMalFormadoEsErrorDeValidacion() throws Exception {
        mockMvc.perform(post("/integrador/clinicalHistory/treatments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(40, "vet", Role.VETERINARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mascotaId\": \"siete\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6617"));
    }

    @Test
    void errorInternoNoExponeDetalles() throws Exception {
        when(clinicalHistoryService.getPetHistory(any(), anyInt()))
                .thenThrow(new RuntimeException("ERROR: relation \"mascota\" does not exist"));

        mockMvc.perform(get("/integrador/clinicalHistory/pets/7").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6615"))
                .andExpect(jsonPath("$.responseObj").value(ErrorMessages.GENERIC_MESSAGE));
    }

    @Test
    void metodoNoPermitidoConservaSuCodigoHttp() throws Exception {
        mockMvc.perform(delete("/integrador/clinicalHistory/pets/7").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.responseCode").value("6620"))
                .andExpect(jsonPath("$.responseObj").value(ErrorMessages.GENERIC_MESSAGE));
    }
}
