package com.taller.backend.core.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Taller Mecánico SaaS API",
                version = "v1.0.0",
                description = "API RESTful Multi-Tenant para gestión integral de talleres mecánicos automotrices, órdenes de trabajo, inventario, clientes y suscripciones.",
                contact = @Contact(
                        name = "Gabriel Isidro Garcia",
                        email = "gabrielisidro8@gmail.com"
                ),
                license = @License(
                        name = "Proprietary",
                        url = "https://tutaller.com"
                )
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Ingrese su token JWT generado en /api/v1/talleres/auth/login o /api/v1/backoffice/auth/login"
)
public class OpenApiConfig {
}
