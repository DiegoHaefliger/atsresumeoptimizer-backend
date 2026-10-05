package com.diegohaefliger.atsresumeoptimizer.ai.application;

import static com.diegohaefliger.atsresumeoptimizer.ai.application.ProviderCredentials.baseUrlOf;
import static com.diegohaefliger.atsresumeoptimizer.ai.application.ProviderCredentials.blankToNull;
import static com.diegohaefliger.atsresumeoptimizer.ai.application.ProviderCredentials.hint;

import com.diegohaefliger.atsresumeoptimizer.ai.domain.InvalidAiSettingsException;
import com.github.f4b6a3.uuid.UuidCreator;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
class AiSettingsServiceImpl implements AiSettingsService {

	private static final Comparator<AiProviderView> CHAIN_ORDER = Comparator
			.comparing((AiProviderView view) -> !view.enabled())
			.thenComparing(view -> view.priority() == null ? Integer.MAX_VALUE : view.priority());

	private final AiSettingsRepository settingsRepository;
	private final AiProviderConfigRepository providerConfigRepository;
	private final AiRuntimeSettingsProvider runtimeSettings;
	private final ProviderCredentials credentials;
	private final AiConnectionTester connectionTester;
	private final AiModelGateway modelGateway;
	private final SettingsSecretStore secretStore;

	AiSettingsServiceImpl(
			AiSettingsRepository settingsRepository,
			AiProviderConfigRepository providerConfigRepository,
			AiRuntimeSettingsProvider runtimeSettings,
			ProviderCredentials credentials,
			AiConnectionTester connectionTester,
			AiModelGateway modelGateway,
			SettingsSecretStore secretStore) {
		this.settingsRepository = settingsRepository;
		this.providerConfigRepository = providerConfigRepository;
		this.runtimeSettings = runtimeSettings;
		this.credentials = credentials;
		this.connectionTester = connectionTester;
		this.modelGateway = modelGateway;
		this.secretStore = secretStore;
	}

	@Override
	@Transactional(readOnly = true)
	public AiSettingsView current() {
		AiRuntimeSettingsProvider.GenerationParameters parameters = runtimeSettings.parameters();
		Map<AiProvider, AiProviderConfigEntity> configs = providerConfigRepository.findAll().stream()
				.collect(Collectors.toMap(AiProviderConfigEntity::provider, Function.identity()));
		List<AiProviderView> providers = Arrays.stream(AiProvider.values())
				.map(provider -> providerView(provider, Optional.ofNullable(configs.get(provider))))
				.sorted(CHAIN_ORDER)
				.toList();
		return new AiSettingsView(BigDecimal.valueOf(parameters.temperature()), parameters.maxOutputTokens(),
				(int) parameters.timeout().toSeconds(),
				secretStore.canEncrypt(),
				providers);
	}

	@Override
	@Transactional
	public AiSettingsView save(AiSettingsUpdate update) {
		List<AiProviderUpdate> enabled = update.providers().stream().filter(AiProviderUpdate::enabled).toList();
		if (enabled.isEmpty()) {
			throw new InvalidAiSettingsException("Deixe pelo menos um provedor ativo.");
		}
		long distinct = update.providers().stream().map(AiProviderUpdate::provider).distinct().count();
		if (distinct != update.providers().size()) {
			throw new InvalidAiSettingsException("Cada provedor só pode aparecer uma vez na lista.");
		}
		for (AiProviderUpdate provider : update.providers()) {
			saveProvider(provider, enabled.indexOf(provider) + 1);
		}

		AiSettingsEntity settings = settingsRepository.findFirstByOrderByUpdatedAtDesc()
				.orElseGet(() -> new AiSettingsEntity(UuidCreator.getTimeOrderedEpoch()));
		settings.update(update.temperature(), update.maxOutputTokens(), update.timeoutSeconds());
		settingsRepository.save(settings);

		modelGateway.evictAll();
		return current();
	}

	@Override
	@Transactional(readOnly = true)
	public AiConnectionTestResult test(AiProviderTestCommand command) {
		return connectionTester.test(command);
	}

	@Override
	@Transactional(readOnly = true)
	public AiModelList listModels(AiModelQuery query) {
		return connectionTester.listModels(query);
	}

	private void saveProvider(AiProviderUpdate update, int priority) {
		Optional<AiProviderConfigEntity> existing = providerConfigRepository.findById(update.provider());
		boolean touched = update.enabled() || StringUtils.hasText(update.apiKey());
		if (existing.isEmpty() && !touched) {
			return;
		}
		AiProviderConfigEntity config = existing.orElseGet(() -> new AiProviderConfigEntity(update.provider()));
		String baseUrl = baseUrlOf(update.provider(), update.baseUrl());
		if (!StringUtils.hasText(update.apiKey()) && config.apiKeyEncrypted() != null && !Objects.equals(config.baseUrl(), baseUrl)) {
			throw new InvalidAiSettingsException(update.provider().label() + ": " + ProviderCredentials.BASE_URL_CHANGED);
		}
		if (StringUtils.hasText(update.apiKey())) {
			String apiKey = update.apiKey().strip();
			config.replaceApiKey(credentials.encrypt(apiKey), hint(apiKey));
		}
		if (update.enabled()) {
			validateEnabled(update, config, baseUrl);
			config.enable(priority);
		} else {
			config.disable();
		}
		String model = StringUtils.hasText(update.model())
				? update.model().strip()
				: Optional.ofNullable(config.model()).orElse(update.provider().defaultModel());
		if (!StringUtils.hasText(model)) {
			return;
		}
		config.update(baseUrl, model, blankToNull(update.heavyModel()));
		providerConfigRepository.save(config);
	}

	private void validateEnabled(AiProviderUpdate update, AiProviderConfigEntity config, String baseUrl) {
		String label = update.provider().label();
		if (!StringUtils.hasText(update.model())) {
			throw new InvalidAiSettingsException(label + ": informe o modelo.");
		}
		if (update.provider().requiresBaseUrl() && baseUrl == null) {
			throw new InvalidAiSettingsException(label + ": informe a URL base.");
		}
		if (update.provider().requiresApiKey() && config.apiKeyEncrypted() == null) {
			throw new InvalidAiSettingsException(label + ": informe a chave de API.");
		}
	}

	private AiProviderView providerView(AiProvider provider, Optional<AiProviderConfigEntity> config) {
		boolean hasKey = config.map(AiProviderConfigEntity::apiKeyEncrypted).isPresent();
		return new AiProviderView(provider, provider.label(), config.map(AiProviderConfigEntity::enabled).orElse(false),
				config.map(AiProviderConfigEntity::priority).orElse(null), provider.requiresApiKey(),
				provider.requiresBaseUrl(), provider.defaultBaseUrl(), provider.suggestedModels(),
				config.map(AiProviderConfigEntity::model).orElse(provider.defaultModel()),
				config.map(AiProviderConfigEntity::heavyModel).orElse(provider.defaultHeavyModel()),
				config.map(AiProviderConfigEntity::baseUrl).orElse(provider.defaultBaseUrl()),
				hasKey ? ApiKeySource.DATABASE : ApiKeySource.NONE,
				hasKey ? config.get().apiKeyHint() : null);
	}
}
