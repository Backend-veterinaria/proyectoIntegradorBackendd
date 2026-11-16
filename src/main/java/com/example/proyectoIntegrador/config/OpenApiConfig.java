package com.example.proyectoIntegrador.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación de la API (QA-05): Swagger UI en /swagger-ui.html, especificación OpenAPI en /v3/api-docs.
 * En Swagger UI: llamar a POST /integrador/login, copiar el token y pegarlo en el botón "Authorize".
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI apiDocumentation() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Veterinaria - Proyecto Integrador")
                        .version("1.0")
                        .description("""
                                Gestión de citas, mascotas, propietarios, veterinarios, historia clínica y reportes.

                                **Autenticación:** POST /integrador/login devuelve un `token`. Pulse *Authorize* y pegue
                                el token (sin la palabra Bearer). Login y registro no lo necesitan.

                                **Respuestas:** todas tienen la forma `{responseCode, responseDesc, responseObj}`.
                                El éxito se reconoce por `responseCode = "0000"`; los errores de negocio llegan con HTTP 200
                                y otro código. 401 (código 6618) = sin token o vencido; 403 (6619) = sin permiso.

                                **Fechas** enviadas como `yyyyMMdd` y horas como `HHmmss`."""))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
