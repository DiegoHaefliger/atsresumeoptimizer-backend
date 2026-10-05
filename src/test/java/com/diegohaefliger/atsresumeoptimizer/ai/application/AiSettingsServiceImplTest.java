package com.diegohaefliger.atsresumeoptimizer.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.domain.InvalidAiSettingsException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiSettingsServiceImplTest {

	@Mock
	private AiSettingsRepository settingsRepository;
	@Mock
	private AiProviderConfigRepository providerConfigRepository;
	@Mock
	private AiConnectionTester connectionTester;
	@Mock
	private AiModelGateway modelGateway;

	private final AiProperties properties = ApiKeyCipherTest.properties("segredo");
	private final ApiKeyCipher cipher = new ApiKeyCipher(new SettingsSecretStore(properties));
	private final Map<AiProvider, AiProviderConfigEntity> stored = new HashMap<>();
	private AiSettingsServiceImpl service;

	@BeforeEach
	void setUp() {
		var runtimeSettings = new AiRuntimeSettingsProvider(settingsRepository, providerConfigRepository, cipher, properties);
		var credentials = new ProviderCredentials(providerConfigRepository, cipher);
		service = new AiSettingsServiceImpl(settingsRepository, providerConfigRepository, runtimeSettings, credentials,
				connectionTester, modelGateway, new SettingsSecretStore(properties));
		lenient().when(settingsRepository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.empty());
		lenient().when(providerConfigRepository.findById(any()))
				.thenAnswer(invocation -> Optional.ofNullable(stored.get(invocation.<AiProvider>getArgument(0))));
		lenient().when(providerConfigRepository.save(any())).thenAnswer(invocation -> {
			AiProviderConfigEntity config = invocation.getArgument(0);
			stored.put(config.provider(), config);
			return config;
		});
		lenient().when(providerConfigRepository.findAll()).thenAnswer(invocation -> new ArrayList<>(stored.values()));
	}

	@Test
	void savesTheEnabledProvidersInTheGivenOrderAsPriorityAndEncryptsNewKeys() {
		service.save(update(
				provider(AiProvider.ANTHROPIC, true, "sk-ant-secret-9876", "claude-sonnet-5-5"),
				provider(AiProvider.GEMINI, false, null, null),
				provider(AiProvider.OPENAI, true, "sk-openai-1", "gpt-4o-mini")));

		assertThat(stored.get(AiProvider.ANTHROPIC).priority()).isEqualTo(1);
		assertThat(stored.get(AiProvider.OPENAI).priority()).isEqualTo(2);
		assertThat(stored).doesNotContainKey(AiProvider.GEMINI);
		assertThat(stored.get(AiProvider.ANTHROPIC).apiKeyEncrypted()).doesNotContain("sk-ant-secret-9876");
		assertThat(cipher.decrypt(stored.get(AiProvider.ANTHROPIC).apiKeyEncrypted())).isEqualTo("sk-ant-secret-9876");
		assertThat(stored.get(AiProvider.ANTHROPIC).apiKeyHint()).isEqualTo("••••9876");
		verify(modelGateway).evictAll();
	}

	@Test
	void disablesAProviderWithoutLosingItsKey() {
		service.save(update(provider(AiProvider.ANTHROPIC, true, "sk-ant-1111", "claude-sonnet-5-5")));

		service.save(update(
				provider(AiProvider.OPENAI, true, "sk-openai-1", "gpt-4o-mini"),
				provider(AiProvider.ANTHROPIC, false, null, "claude-sonnet-5-5")));

		assertThat(stored.get(AiProvider.ANTHROPIC).enabled()).isFalse();
		assertThat(stored.get(AiProvider.ANTHROPIC).apiKeyEncrypted()).isNotNull();
		assertThat(stored.get(AiProvider.OPENAI).priority()).isEqualTo(1);
	}

	@Test
	void requiresAtLeastOneEnabledProvider() {
		assertThatThrownBy(() -> service.save(update(provider(AiProvider.OPENAI, false, null, "gpt-4o-mini"))))
				.isInstanceOf(InvalidAiSettingsException.class);
		verify(settingsRepository, never()).save(any());
	}

	@Test
	void requiresAKeyForAnEnabledProviderThatHasNoneSaved() {
		assertThatThrownBy(() -> service.save(update(provider(AiProvider.GEMINI, true, null, "gemini-2.5-flash"))))
				.isInstanceOf(InvalidAiSettingsException.class)
				.hasMessageContaining("Google Gemini");
	}

	@Test
	void refusesToSendTheSavedKeyToANewBaseUrl() {
		AiProviderConfigEntity saved = new AiProviderConfigEntity(AiProvider.OPENAI_COMPATIBLE);
		saved.replaceApiKey(cipher.encrypt("gsk-123456"), "••••3456");
		saved.update("https://api.groq.com/openai/v1", "llama-3.3-70b", null);
		stored.put(AiProvider.OPENAI_COMPATIBLE, saved);

		assertThatThrownBy(() -> service.save(update(new AiProviderUpdate(AiProvider.OPENAI_COMPATIBLE, true, null,
				"https://outro-servidor.example/v1", "llama-3.3-70b", null))))
				.isInstanceOf(InvalidAiSettingsException.class)
				.hasMessageContaining("URL base");
	}

	@Test
	void showsNoProviderEnabledAndNoKeyWhileNothingWasSavedOnTheScreen() {
		AiSettingsView view = service.current();

		assertThat(view.providers()).noneMatch(AiProviderView::enabled);
		assertThat(view.providers()).extracting(AiProviderView::apiKeySource).containsOnly(ApiKeySource.NONE);
		AiProviderView openAi = view.providers().stream()
				.filter(provider -> provider.provider() == AiProvider.OPENAI).findFirst().orElseThrow();
		assertThat(openAi.model()).isEqualTo(AiProvider.OPENAI.defaultModel());
	}

	@Test
	void listsTheEnabledProvidersFirstInPriorityOrder() {
		service.save(update(
				provider(AiProvider.ANTHROPIC, true, "sk-ant-1", "claude-sonnet-5-5"),
				provider(AiProvider.OPENAI, true, "sk-openai-1", "gpt-4o-mini")));

		AiSettingsView view = service.current();

		assertThat(view.providers()).extracting(AiProviderView::provider)
				.startsWith(AiProvider.ANTHROPIC, AiProvider.OPENAI);
	}

	private AiProviderUpdate provider(AiProvider provider, boolean enabled, String apiKey, String model) {
		return new AiProviderUpdate(provider, enabled, apiKey, null, model, null);
	}

	private AiSettingsUpdate update(AiProviderUpdate... providers) {
		return new AiSettingsUpdate(new BigDecimal("0.10"), 2000, 120, List.of(providers));
	}
}
