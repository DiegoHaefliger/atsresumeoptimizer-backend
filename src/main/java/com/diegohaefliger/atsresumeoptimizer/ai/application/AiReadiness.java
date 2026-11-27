package com.diegohaefliger.atsresumeoptimizer.ai.application;

import com.diegohaefliger.atsresumeoptimizer.diagnostics.ReadinessCheck;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.ReadinessContributor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
class AiReadiness implements ReadinessContributor {

	private static final String CONFIGURED = "IA configurada";
	private static final String SETTINGS_HINT = "Configure em Configurações > Inteligência artificial.";
	private static final String OLLAMA_DOWN_HINT = "Abra o Ollama e confira o endereço em Configurações > Inteligência artificial.";
	private static final String CLOUD_DOWN_HINT = "Confira a chave de API e a conexão com a internet.";
	private static final String OLLAMA_DEFAULT_TAG = ":latest";

	private final AiSettingsService settingsService;
	private final ProviderCredentials credentials;
	private final AiModelCatalog modelCatalog;

	AiReadiness(AiSettingsService settingsService, ProviderCredentials credentials, AiModelCatalog modelCatalog) {
		this.settingsService = settingsService;
		this.credentials = credentials;
		this.modelCatalog = modelCatalog;
	}

	@Override
	public List<ReadinessCheck> checks() {
		List<AiProviderView> enabled = settingsService.current().providers().stream()
				.filter(AiProviderView::enabled)
				.sorted(Comparator.comparing(AiProviderView::priority, Comparator.nullsLast(Comparator.naturalOrder())))
				.toList();
		if (enabled.isEmpty()) {
			return List.of(ReadinessCheck.failure(CONFIGURED, "nenhum provedor de IA ativo", SETTINGS_HINT));
		}
		List<ReadinessCheck> checks = new ArrayList<>();
		checks.add(ReadinessCheck.ok(CONFIGURED,
				enabled.stream().map(AiProviderView::label).collect(Collectors.joining(", "))));
		enabled.forEach(provider -> checks.add(connection(provider)));
		return checks;
	}

	private ReadinessCheck connection(AiProviderView provider) {
		String name = "Conexão com " + provider.label();
		if (provider.requiresApiKey() && provider.apiKeySource() == ApiKeySource.NONE) {
			return ReadinessCheck.failure(name, "sem chave de API", SETTINGS_HINT);
		}
		String baseUrl = ProviderCredentials.baseUrlOf(provider.provider(),
				provider.baseUrl() != null ? provider.baseUrl() : provider.defaultBaseUrl());
		if (provider.requiresBaseUrl() && baseUrl == null) {
			return ReadinessCheck.failure(name, "sem endereço do provedor", SETTINGS_HINT);
		}
		long start = System.nanoTime();
		List<String> available;
		try {
			available = modelCatalog.list(provider.provider(), credentials.resolveApiKey(provider.provider(), null, provider.baseUrl()),
					baseUrl);
		} catch (RuntimeException exception) {
			return ReadinessCheck.failure(name, "não respondeu: " + exception.getMessage(), downHint(provider));
		}
		long millis = Duration.ofNanos(System.nanoTime() - start).toMillis();
		List<String> missing = configuredModels(provider).filter(model -> !isAvailable(model, available)).toList();
		if (!available.isEmpty() && !missing.isEmpty()) {
			return ReadinessCheck.warning(name, "respondeu, mas o modelo %s não está disponível".formatted(String.join(", ", missing)),
					missingModelHint(provider, missing));
		}
		return ReadinessCheck.ok(name, "respondeu em %d ms".formatted(millis));
	}

	private static Stream<String> configuredModels(AiProviderView provider) {
		return Stream.of(provider.model(), provider.heavyModel()).filter(Objects::nonNull).map(String::strip)
				.filter(model -> !model.isEmpty()).distinct();
	}

	private static boolean isAvailable(String model, List<String> available) {
		return available.contains(model) || available.contains(model + OLLAMA_DEFAULT_TAG);
	}

	private static String downHint(AiProviderView provider) {
		return provider.provider() == AiProvider.OLLAMA ? OLLAMA_DOWN_HINT : CLOUD_DOWN_HINT;
	}

	private static String missingModelHint(AiProviderView provider, List<String> missing) {
		if (provider.provider() == AiProvider.OLLAMA) {
			return missing.stream().map(model -> "ollama pull " + model).collect(Collectors.joining(" e ", "Baixe no terminal: ", "."));
		}
		return "Escolha um modelo disponível em Configurações > Inteligência artificial.";
	}
}
