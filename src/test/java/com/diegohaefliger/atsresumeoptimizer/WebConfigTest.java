package com.diegohaefliger.atsresumeoptimizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class WebConfigTest {

	private static final String EXTENSION = "chrome-extension://abcdefghijklmnop";
	private static final String APP = "http://localhost:5173";

	private Map<String, CorsConfiguration> configurations;

	@BeforeEach
	void registerMappings() {
		WebConfig config = new WebConfig();
		ReflectionTestUtils.setField(config, "allowedOrigins", List.of(APP));
		ExposedCorsRegistry registry = new ExposedCorsRegistry();
		config.addCorsMappings(registry);
		configurations = registry.exposed();
	}

	@Test
	void acceptsExtensionOnSnapshotUploadWithPairingHeader() {
		CorsConfiguration cors = configurations.get("/api/v1/linkedin/snapshots");

		assertThat(cors.checkOrigin(EXTENSION)).isEqualTo(EXTENSION);
		assertThat(cors.checkHeaders(List.of("Content-Type", "X-Pairing-Token"))).hasSize(2);
	}

	@Test
	void acceptsExtensionOnPairingTokenRead() {
		assertThat(configurations.get("/api/v1/linkedin/pairing-token").checkOrigin(EXTENSION)).isEqualTo(EXTENSION);
	}

	@Test
	void keepsExtensionOutOfEveryOtherRoute() {
		assertThat(configurations.get("/**").checkOrigin(EXTENSION)).isNull();
		assertThat(configurations.get("/**").checkOrigin(APP)).isEqualTo(APP);
	}

	private static class ExposedCorsRegistry extends CorsRegistry {
		Map<String, CorsConfiguration> exposed() {
			return getCorsConfigurations();
		}
	}
}
