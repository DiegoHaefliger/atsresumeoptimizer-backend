package com.diegohaefliger.atsresumeoptimizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.ServletRequestPathUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class WebConfigTest {

	private static final String EXTENSION = "chrome-extension://abcdefghijklmnop";
	private static final String APP = "http://localhost:5173";

	private UrlBasedCorsConfigurationSource source;

	@BeforeEach
	void registerMappings() {
		registerMappings(List.of(APP));
	}

	private void registerMappings(List<String> allowedOrigins) {
		WebConfig config = new WebConfig();
		ReflectionTestUtils.setField(config, "allowedOrigins", allowedOrigins);
		ExposedCorsRegistry registry = new ExposedCorsRegistry();
		config.addCorsMappings(registry);
		source = new UrlBasedCorsConfigurationSource();
		source.setCorsConfigurations(registry.exposed());
	}

	@Test
	void acceptsExtensionOnSnapshotUploadWithPairingHeader() {
		CorsConfiguration cors = resolve("/api/v1/linkedin/snapshots");

		assertThat(cors.checkOrigin(EXTENSION)).isEqualTo(EXTENSION);
		assertThat(cors.checkHeaders(List.of("Content-Type", "X-Pairing-Token"))).hasSize(2);
	}

	@Test
	void acceptsExtensionOnPairingTokenRead() {
		assertThat(resolve("/api/v1/linkedin/pairing-token").checkOrigin(EXTENSION)).isEqualTo(EXTENSION);
	}

	@Test
	void keepsExtensionOutOfEveryOtherRoute() {
		assertThat(resolve("/api/v1/jobs").checkOrigin(EXTENSION)).isNull();
		assertThat(resolve("/api/v1/jobs").checkOrigin(APP)).isEqualTo(APP);
	}

	@Test
	void acceptsViteOnWhateverLocalPortItPicked() {
		registerMappings(List.of("http://localhost:[*]"));

		assertThat(resolve("/api/v1/jobs").checkOrigin("http://localhost:5175")).isEqualTo("http://localhost:5175");
		assertThat(resolve("/api/v1/jobs").checkOrigin("http://evil.example")).isNull();
	}

	private CorsConfiguration resolve(String path) {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
		ServletRequestPathUtils.parseAndCache(request);
		return source.getCorsConfiguration(request);
	}

	private static class ExposedCorsRegistry extends CorsRegistry {
		Map<String, CorsConfiguration> exposed() {
			return getCorsConfigurations();
		}
	}
}
