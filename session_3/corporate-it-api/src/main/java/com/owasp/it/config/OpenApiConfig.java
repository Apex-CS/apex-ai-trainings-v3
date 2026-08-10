package com.owasp.it.config;

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

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI itOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("OWASP Corporate IT API")
                        .description("""
                                Demo IT operations API secured with JWT bearer tokens.
                                Use the **Authorize** button and paste a token from \
                                `corporate-backend-financial-api/DEMO_TOKENS.md`.
                                """)
                        .version("0.1.0")
                        .contact(new Contact().name("OWASP Demo")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Shared demo JWT from corporate-backend-financial-api/DEMO_TOKENS.md")));
    }
}
