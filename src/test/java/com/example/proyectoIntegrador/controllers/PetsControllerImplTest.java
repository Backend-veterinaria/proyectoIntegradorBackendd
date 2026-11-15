package com.example.proyectoIntegrador.controllers;

import com.example.proyectoIntegrador.controllers.Impl.PetsControllerImpl;
import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.InfoMascota;
import com.example.proyectoIntegrador.security.AuthenticatedUser;
import com.example.proyectoIntegrador.security.JwtService;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.security.SecurityConfig;
import com.example.proyectoIntegrador.services.PetsServices;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PetsControllerImpl.class)
@Import({SecurityConfig.class, JwtService.class})
class PetsControllerImplTest {

    private static final String UPDATE_BODY = "{\"nombre\":\"Luna\",\"edad\":4,\"genero\":\"Hembra\",\"razaId\":2}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private PetsServices petsServices;

    private String bearer(int userId, Role role) {
        return "Bearer " + jwtService.generateToken(userId, "usuario", role);
    }

    @Test
    void clienteEditaMascota() throws Exception {
        InfoMascota pet = new InfoMascota();
        pet.setIdMascota(8);
        pet.setNombreMascota("Luna");
        when(petsServices.updatePet(any(), eq(8), any())).thenReturn(pet);

        mockMvc.perform(put("/integrador/pets/8")
                        .header(HttpHeaders.AUTHORIZATION, bearer(5, Role.CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.responseObj.nombreMascota").value("Luna"));

        verify(petsServices).updatePet(eq(new AuthenticatedUser(5, "usuario", Role.CLIENTE)), eq(8), any());
    }

    @Test
    void editarMascotaAjenaDevuelve403() throws Exception {
        when(petsServices.updatePet(any(), anyInt(), any()))
                .thenThrow(new AccessDeniedException("Solo puede gestionar sus propias mascotas"));

        mockMvc.perform(put("/integrador/pets/8")
                        .header(HttpHeaders.AUTHORIZATION, bearer(5, Role.CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.responseCode").value("6619"));
    }

    @Test
    void editarConDatosInvalidosDevuelveErrorDeValidacion() throws Exception {
        mockMvc.perform(put("/integrador/pets/8")
                        .header(HttpHeaders.AUTHORIZATION, bearer(5, Role.CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"edad\":-1,\"genero\":\"Hembra\",\"razaId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6617"))
                .andExpect(jsonPath("$.responseObj", hasItem("edad: edad must be zero or positive")));

        verify(petsServices, never()).updatePet(any(), anyInt(), any());
    }

    @Test
    void errorDeNegocioAlEditarSeMuestra() throws Exception {
        when(petsServices.updatePet(any(), anyInt(), any()))
                .thenThrow(new BusinessException("El cliente ya tiene otra mascota con esos datos"));

        mockMvc.perform(put("/integrador/pets/8")
                        .header(HttpHeaders.AUTHORIZATION, bearer(5, Role.CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("6616"))
                .andExpect(jsonPath("$.responseObj").value("El cliente ya tiene otra mascota con esos datos"));
    }

    @Test
    void inactivarMascota() throws Exception {
        InfoMascota pet = new InfoMascota();
        pet.setActivo(false);
        when(petsServices.changeStatus(any(), eq(8), eq(false))).thenReturn(pet);

        mockMvc.perform(patch("/integrador/pets/8/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(5, Role.CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseObj.activo").value(false));
    }

    @Test
    void cambiarEstadoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(patch("/integrador/pets/8/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isUnauthorized());

        verify(petsServices, never()).changeStatus(any(), anyInt(), anyBoolean());
    }

    @Test
    void listarIncluyendoInactivas() throws Exception {
        when(petsServices.getListPets(any(), eq(true))).thenReturn(List.of());

        mockMvc.perform(get("/integrador/pets/getListPets").param("idClient", "5").param("includeInactive", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer(5, Role.CLIENTE)))
                .andExpect(status().isOk());

        verify(petsServices).getListPets(any(), eq(true));
    }

    @Test
    void listarPorDefectoSoloActivas() throws Exception {
        when(petsServices.getListPets(any(), eq(false))).thenReturn(List.of());

        mockMvc.perform(get("/integrador/pets/getListPets").param("idClient", "5")
                        .header(HttpHeaders.AUTHORIZATION, bearer(5, Role.CLIENTE)))
                .andExpect(status().isOk());

        verify(petsServices).getListPets(any(), eq(false));
    }
}
