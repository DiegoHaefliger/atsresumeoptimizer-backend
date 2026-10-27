package com.diegohaefliger.atsresumeoptimizer;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebConfig implements WebMvcConfigurer {

	private static final String EXTENSION_ORIGIN = "chrome-extension://*";
	private static final String PAIRING_HEADER = "X-Pairing-Token";

	@Value("${app.cors.allowed-origins}")
	private List<String> allowedOrigins;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/v1/linkedin/snapshots")
				.allowedOriginPatterns(EXTENSION_ORIGIN)
				.allowedMethods("POST")
				.allowedHeaders("Content-Type", PAIRING_HEADER);
		registry.addMapping("/api/v1/linkedin/pairing-token")
				.allowedOriginPatterns(EXTENSION_ORIGIN)
				.allowedMethods("GET");
		registry.addMapping("/**")
				.allowedOrigins(allowedOrigins.toArray(String[]::new))
				.allowedMethods("GET", "POST", "PUT", "DELETE")
				.allowedHeaders("Content-Type")
				.exposedHeaders(HttpHeaders.CONTENT_DISPOSITION);
	}
}
