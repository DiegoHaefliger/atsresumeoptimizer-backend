package com.diegohaefliger.atsresumeoptimizer.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.diagnostics.ReadinessCheck;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.ReadinessStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiReadinessTest {

	private static final String OLLAMA_URL = "http://localhost:11434";

	private final AiSettingsService settingsService = mock(AiSettingsService.class);
	private final ProviderCredentials credentials = mock(ProviderCredentials.class);
	private final AiModelCatalog modelCatalog = mock(AiModelCatalog.class);
	private final AiReadiness readiness = new AiReadiness(settingsService, credentials, modelCatalog);

	private static AiProviderView ollama(String model) {
		return new AiProviderView(AiProvider.OLLAMA, "Ollama (local)", true, 1, false, true, OLLAMA_URL, List.of(), model,
				null, null, ApiKeySource.NONE, null);
	}

	private static AiProviderView openAi(ApiKeySource keySource) {
		return new AiProviderView(AiProvider.OPENAI, "OpenAI", true, 2, true, false, null, List.of(), "gpt-4o-mini", null,
				null, keySource, null);
	}

	private void configured(AiProviderView... providers) {
		when(settingsService.current()).thenReturn(new AiSettingsView(BigDecimal.ZERO, 2000, 120, true, List.of(providers)));
	}

	@Test
	void failsWhenNoProviderIsActive() {
		configured();

		assertThat(readiness.checks()).extracting(ReadinessCheck::status).containsExactly(ReadinessStatus.FAILURE);
	}

	@Test
	void passesWhenTheOllamaModelIsInstalled() {
		configured(ollama("llama3.1"));
		when(modelCatalog.list(eq(AiProvider.OLLAMA), any(), eq(OLLAMA_URL))).thenReturn(List.of("llama3.1:latest"));

		assertThat(readiness.checks()).extracting(ReadinessCheck::status).containsExactly(ReadinessStatus.OK, ReadinessStatus.OK);
	}

	@Test
	void tellsHowToDownloadAMissingOllamaModel() {
		configured(ollama("qwen2.5:14b"));
		when(modelCatalog.list(eq(AiProvider.OLLAMA), any(), eq(OLLAMA_URL))).thenReturn(List.of("llama3.1:8b"));

		ReadinessCheck connection = readiness.checks().get(1);

		assertThat(connection.status()).isEqualTo(ReadinessStatus.WARNING);
		assertThat(connection.hint()).contains("ollama pull qwen2.5:14b");
	}

	@Test
	void failsWhenOllamaIsNotRunning() {
		configured(ollama("llama3.1:8b"));
		when(modelCatalog.list(eq(AiProvider.OLLAMA), any(), eq(OLLAMA_URL))).thenThrow(new IllegalStateException("Connection refused"));

		ReadinessCheck connection = readiness.checks().get(1);

		assertThat(connection.status()).isEqualTo(ReadinessStatus.FAILURE);
		assertThat(connection.detail()).contains("Connection refused");
	}

	@Test
	void failsACloudProviderWithoutApiKeyWithoutCallingIt() {
		configured(openAi(ApiKeySource.NONE));

		ReadinessCheck connection = readiness.checks().get(1);

		assertThat(connection.status()).isEqualTo(ReadinessStatus.FAILURE);
		assertThat(connection.detail()).isEqualTo("sem chave de API");
	}
}
