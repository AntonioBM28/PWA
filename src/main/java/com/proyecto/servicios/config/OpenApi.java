package com.proyecto.servicios.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentacion OpenAPI. Los endpoints protegidos usan el esquema "bearerAuth": en Swagger UI se
 * obtiene el token con POST /auth/login y se pega en el boton Authorize. Los endpoints publicos
 * se marcan con @SecurityRequirements vacio.
 */
@Configuration
public class OpenApi {

    static final String ESQUEMA_SEGURIDAD = "bearerAuth";

    @Bean
    public OpenAPI openAPI(){
        return new OpenAPI()
                .info(new Info()
                        .title("Servicios Proyecto")
                        .description("Onboarding de clientes personas fisicas y catalogo de productos GestoPago")
                        .version("1.0"))
                .components(new Components().addSecuritySchemes(ESQUEMA_SEGURIDAD, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token obtenido en POST /auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_SEGURIDAD));
    }
}
