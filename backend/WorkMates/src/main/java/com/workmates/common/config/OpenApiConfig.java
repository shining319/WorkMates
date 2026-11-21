package com.workmates.common.config;

import com.workmates.common.security.CurrentUser;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI is the team's API reference ({@code /swagger-ui.html}). This declares the Bearer scheme,
 * so testers can paste a token with the "Authorize" button and call protected endpoints.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    // CurrentUser comes from the token (CurrentUserArgumentResolver), not from the request. Without this,
    // springdoc lists it as a required query parameter "user" on every endpoint that takes it.
    static {
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(CurrentUser.class);
    }

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
