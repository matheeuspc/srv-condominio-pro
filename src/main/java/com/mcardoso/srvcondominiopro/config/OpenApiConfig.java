package com.mcardoso.srvcondominiopro.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI condominioProOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CondomínioPro API")
                        .version("v1")
                        .description("""
                                API do CondomínioPro (SaaS de gestão de condomínios).
                                Base: `/api/v1`. Autenticação por JWT: faça `POST /api/v1/auth/login`,
                                copie o `token` e clique em **Authorize**.
                                """)
                        .contact(new Contact().name("CondomínioPro")))
                // Aplica o esquema de segurança globalmente; os endpoints públicos
                // (auth/login, register, forgot/reset/refresh, webhook, convite) funcionam sem token.
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
