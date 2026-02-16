package com.wiki.monowiki.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
	return new OpenAPI()
		.info(new Info()
			.title("Mini Team Wiki API")
			.version("v1")
			.description("All endpoints return BaseResponse / BasePageResponse wrappers. "
				     + "Authenticate via /auth/login and use Swagger's Authorize button (Bearer JWT)."))
		.components(new Components().addSecuritySchemes(
			BEARER_AUTH,
			new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT")
		))
		// makes JWT apply by default to all endpoints (Swagger can still call /auth/login without token)
		.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
