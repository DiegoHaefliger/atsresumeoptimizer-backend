package com.diegohaefliger.atsresumeoptimizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

	@Bean
	OpenAPI atsResumeOptimizerOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("ATS Resume Optimizer API")
						.description("Score explicável de aderência a ATS e sugestões de adaptação de currículo.")
						.version("v1"))
				.components(new Components().addSecuritySchemes("apiKey", new SecurityScheme()
						.type(SecurityScheme.Type.APIKEY)
						.in(SecurityScheme.In.HEADER)
						.name("X-API-Key")
						.description("Chave das rotas /api/v1/integrations/**, definida em INTEGRATION_API_KEY.")));
	}
}
