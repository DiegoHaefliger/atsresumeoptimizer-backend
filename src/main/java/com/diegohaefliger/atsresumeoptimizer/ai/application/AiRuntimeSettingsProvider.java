package com.diegohaefliger.atsresumeoptimizer.ai.application;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
class AiRuntimeSettingsProvider {

	private final AiSettingsRepository settingsRepository;
	private final AiProviderConfigRepository providerConfigRepository;
	private final ApiKeyCipher cipher;
	private final AiProperties properties;
	private final AtomicReference<List<AiRuntimeSettings>> current = new AtomicReference<>();

	AiRuntimeSettingsProvider(
			AiSettingsRepository settingsRepository,
			AiProviderConfigRepository providerConfigRepository,
			ApiKeyCipher cipher,
			AiProperties properties) {
		this.settingsRepository = settingsRepository;
		this.providerConfigRepository = providerConfigRepository;
		this.cipher = cipher;
		this.properties = properties;
	}

	List<AiRuntimeSettings> chain() {
		return current.updateAndGet(chain -> chain != null ? chain : load());
	}

	void refresh() {
		current.set(null);
	}

	private List<AiRuntimeSettings> load() {
		GenerationParameters parameters = parameters();
		return providerConfigRepository.findByEnabledTrueOrderByPriorityAsc().stream()
				.map(config -> settingsOf(config, parameters))
				.toList();
	}

	GenerationParameters parameters() {
		return settingsRepository.findFirstByOrderByUpdatedAtDesc()
				.map(saved -> new GenerationParameters(saved.temperature().doubleValue(), saved.maxOutputTokens(),
						Duration.ofSeconds(saved.timeoutSeconds())))
				.orElseGet(() -> new GenerationParameters(properties.temperature(), properties.maxOutputTokens(),
						properties.timeout()));
	}

	private AiRuntimeSettings settingsOf(AiProviderConfigEntity config, GenerationParameters parameters) {
		AiProvider provider = config.provider();
		return new AiRuntimeSettings(
				provider,
				config.apiKeyEncrypted() != null ? cipher.decrypt(config.apiKeyEncrypted()) : null,
				config.baseUrl() != null ? config.baseUrl() : provider.defaultBaseUrl(),
				config.model() != null ? config.model() : provider.defaultModel(),
				config.heavyModel() != null ? config.heavyModel() : provider.defaultHeavyModel(),
				parameters.temperature(),
				parameters.maxOutputTokens(),
				parameters.timeout());
	}

	record GenerationParameters(double temperature, int maxOutputTokens, Duration timeout) {
	}
}
