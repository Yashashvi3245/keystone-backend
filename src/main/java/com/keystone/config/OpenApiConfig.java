package com.keystone.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures OpenAPI 3 / Swagger UI.
 *
 * Accessible at:
 *   http://localhost:8080/swagger-ui.html   (UI)
 *   http://localhost:8080/api-docs          (JSON)
 *
 * All protected endpoints require a Bearer JWT.
 * Obtain a token via POST /api/auth/login, then click
 * "Authorize" in Swagger UI and paste: Bearer <token>
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI keystoneOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KEYSTONE Field Service API")
                        .version("1.0.0")
                        .description("""
                                **KEYSTONE** — Field Service Management Platform
                                
                                ## Authentication
                                All endpoints (except `/api/auth/login`) require a JWT Bearer token.
                                
                                **Steps:**
                                1. `POST /api/auth/login` with `{ "email": "...", "password": "..." }`
                                2. Copy the returned `token` value
                                3. Click **Authorize** and enter: `Bearer <token>`
                                
                                ## Roles
                                | Role | Capabilities |
                                |------|-------------|
                                | MANAGER | Full access: users, customers, sites, work orders, close/cancel, reports |
                                | DISPATCHER | Create/assign work orders, manage customers & sites |
                                | TECHNICIAN | Own assigned work orders only: start/hold/complete, parts & time |
                                | CUSTOMER | Own organisation's work orders only: read + raise requests |
                                
                                ## Seed Accounts (password: `Password123!`)
                                | Role | Email |
                                |------|-------|
                                | MANAGER | manager@keystone.example.com |
                                | DISPATCHER | dispatcher@keystone.example.com |
                                | TECHNICIAN | tech1@keystone.example.com |
                                | CUSTOMER | customer@keystone.example.com |
                                """)
                        .contact(new Contact()
                                .name("Keystone Engineering")
                                .email("engineering@keystone.example.com")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token obtained from POST /api/auth/login")));
    }
}
