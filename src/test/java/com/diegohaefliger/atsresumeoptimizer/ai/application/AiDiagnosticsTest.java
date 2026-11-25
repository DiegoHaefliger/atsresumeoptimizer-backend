package com.diegohaefliger.atsresumeoptimizer.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticEntry;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticSection;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiDiagnosticsTest {

	private final AiSettingsService settingsService = mock(AiSettingsService.class);

	private static AiProviderView provider(AiProvider provider, boolean enabled, Integer priority, String baseUrl,
			ApiKeySource keySource) {
		return new AiProviderView(provider, provider.label(), enabled, priority, provider.requiresApiKey(),
				provider.requiresBaseUrl(), null, List.of(), "modelo-" + provider.name().toLowerCase(), null, baseUrl, keySource,
				"…abcd");
	}

	@Test
	void listsOnlyActiveProvidersAndKeepsTheirAddressOutOfPublicReports() {
		when(settingsService.current()).thenReturn(new AiSettingsView(BigDecimal.ONE, 2000, 120, true, List.of(
				provider(AiProvider.OLLAMA, true, 2, "http://192.168.0.10:11434", ApiKeySource.NONE),
				provider(AiProvider.OPENAI, true, 1, null, ApiKeySource.NONE),
				provider(AiProvider.GEMINI, false, 3, null, ApiKeySource.DATABASE))));

		DiagnosticSection section = new AiDiagnostics(settingsService).section();

		assertThat(section.entries()).extracting(DiagnosticEntry::label)
				.startsWith("OpenAI", "Ollama (local)", "Endereço Ollama (local)")
				.doesNotContain("Google Gemini");
		assertThat(section.entries().get(0).value()).contains("SEM CHAVE").doesNotContain("abcd");
		assertThat(section.entries()).filteredOn(DiagnosticEntry::personal).extracting(DiagnosticEntry::value)
				.containsExactly("http://192.168.0.10:11434");
	}
}
