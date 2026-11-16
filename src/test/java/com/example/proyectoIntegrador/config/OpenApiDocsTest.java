package com.example.proyectoIntegrador.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA-05: la documentación de la API se genera con todos los controladores y se puede abrir sin token.
 * Levanta la aplicación completa sin base de datos (JdbcTemplate simulado).
 */
@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration")
@AutoConfigureMockMvc
class OpenApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Test
    void especificacionOpenApiEsPublicaYTieneLosEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("API Veterinaria - Proyecto Integrador"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/integrador/login'].post").exists())
                .andExpect(jsonPath("$.paths['/integrador/login'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/integrador/clinicalHistory/pets/{mascotaId}'].get").exists())
                .andExpect(jsonPath("$.paths['/integrador/reports/appointmentsByStatus'].get").exists())
                .andExpect(jsonPath("$.paths['/integrador/veterinarians'].post").exists());
    }

    @Test
    void swaggerUiEsPublica() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("swagger-ui/index.html")));
    }

    @Test
    void elRestoSigueProtegido() throws Exception {
        mockMvc.perform(get("/integrador/clients/5"))
                .andExpect(status().isUnauthorized());
    }
}
