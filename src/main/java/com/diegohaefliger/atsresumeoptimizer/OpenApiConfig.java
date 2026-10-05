package com.diegohaefliger.atsresumeoptimizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
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
						.version("v1"));
	}
}
