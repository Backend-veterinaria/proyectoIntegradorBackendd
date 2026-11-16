package com.example.proyectoIntegrador.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * application-prod.properties: la aplicación arranca con las variables de producción, sin Swagger y sin
 * el usuario de demostración. Las variables se pasan como propiedades (equivalen a las de entorno).
 */
@SpringBootTest(properties = {
        "DB_URL=jdbc:postgresql://localhost:5432/no-se-usa",
        "DB_USERNAME=app",
        "DB_PASSWORD=clave-de-prueba",
        "JWT_SECRET=cHJ1ZWJhLXByb2R1Y2Npb24tY2xhdmUtZGUtMzItYnl0ZXMtb2s=",
        "CORS_ALLOWED_ORIGINS=https://veterinaria.ejemplo.com",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration"
})
@ActiveProfiles("prod")
@AutoConfigureMockMvc
class ProductionProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private PlatformTransactionManager transactionManager;

    @Test
    void swaggerEstaApagado() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
    }

    @Test
    void usuarioDemoNoEntra() throws Exception {
        mockMvc.perform(post("/integrador/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"demo123\"}"))
                .andExpect(jsonPath("$.responseCode").value("1231"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void elRestoSigueProtegido() throws Exception {
        mockMvc.perform(get("/integrador/clients/5")).andExpect(status().isUnauthorized());
    }
}
