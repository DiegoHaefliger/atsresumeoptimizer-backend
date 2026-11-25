package com.diegohaefliger.atsresumeoptimizer.ai.application;

import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticEntry;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticSection;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticsContributor;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
class AiDiagnostics implements DiagnosticsContributor {

	private final AiSettingsService settingsService;

	AiDiagnostics(AiSettingsService settingsService) {
		this.settingsService = settingsService;
	}

	@Override
	public DiagnosticSection section() {
		AiSettingsView settings = settingsService.current();
		List<DiagnosticEntry> providers = settings.providers().stream()
				.filter(AiProviderView::enabled)
				.sorted(Comparator.comparing(AiProviderView::priority, Comparator.nullsLast(Comparator.naturalOrder())))
				.flatMap(AiDiagnostics::describe)
				.toList();
		Stream<DiagnosticEntry> general = Stream.of(
				new DiagnosticEntry("Tempo limite", settings.timeoutSeconds() + "s"),
				new DiagnosticEntry("Limite de saída", settings.maxOutputTokens() + " tokens"),
				new DiagnosticEntry("Criptografia das chaves", settings.encryptionAvailable() ? "disponível" : "indisponível"));
		List<DiagnosticEntry> providerEntries = providers.isEmpty()
				? List.of(new DiagnosticEntry("Provedores ativos", "nenhum"))
				: providers;
		return new DiagnosticSection("Inteligência artificial", Stream.concat(providerEntries.stream(), general).toList());
	}

	private static Stream<DiagnosticEntry> describe(AiProviderView provider) {
		String key = !provider.requiresApiKey() ? "não precisa de chave"
				: provider.apiKeySource() == ApiKeySource.NONE ? "SEM CHAVE" : "chave cadastrada";
		String models = Stream.of(provider.model(), provider.heavyModel()).filter(Objects::nonNull).distinct()
				.reduce((light, heavy) -> light + " / " + heavy).orElse("modelo padrão");
		DiagnosticEntry summary = new DiagnosticEntry(provider.label(), models + " · " + key);
		return provider.baseUrl() == null ? Stream.of(summary)
				: Stream.of(summary, DiagnosticEntry.personal("Endereço " + provider.label(), provider.baseUrl()));
	}
}
