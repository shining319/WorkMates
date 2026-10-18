package com.workmates.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI is the team's API reference ({@code /swagger-ui.html}). This declares the Bearer scheme,
 * so testers can paste a token with the "Authorize" button and call protected endpoints.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI workMatesOpenApi() {
        return new OpenAPI()
                .info(new Info().title("WorkMates API").version("0.0.1"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                // Applies to every endpoint; register and login ignore the token anyway
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
