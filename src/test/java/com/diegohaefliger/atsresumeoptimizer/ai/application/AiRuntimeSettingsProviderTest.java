package com.diegohaefliger.atsresumeoptimizer.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiRuntimeSettingsProviderTest {

	@Mock
	private AiSettingsRepository settingsRepository;
	@Mock
	private AiProviderConfigRepository providerConfigRepository;

	private final AiProperties properties = ApiKeyCipherTest.properties("segredo");
	private final ApiKeyCipher cipher = new ApiKeyCipher(new SettingsSecretStore(properties));

	@Test
	void hasNoProviderWhenNoneWasEnabledOnTheScreen() {
		var provider = new AiRuntimeSettingsProvider(settingsRepository, providerConfigRepository, cipher, properties);
		when(settingsRepository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.empty());
		when(providerConfigRepository.findByEnabledTrueOrderByPriorityAsc()).thenReturn(List.of());

		assertThat(provider.chain()).isEmpty();
	}

	@Test
	void buildsTheChainInPriorityOrderWithDecryptedKeysAndSharedParameters() {
		var provider = new AiRuntimeSettingsProvider(settingsRepository, providerConfigRepository, cipher, properties);
		AiSettingsEntity saved = new AiSettingsEntity(UUID.randomUUID());
		saved.update(new BigDecimal("0.20"), 4000, 90);
		when(settingsRepository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.of(saved));
		when(providerConfigRepository.findByEnabledTrueOrderByPriorityAsc())
				.thenReturn(List.of(config(AiProvider.ANTHROPIC, "sk-ant-xyz", 1), config(AiProvider.GEMINI, "AIza-1", 2)));

		List<AiRuntimeSettings> chain = provider.chain();

		assertThat(chain).extracting(AiRuntimeSettings::provider).containsExactly(AiProvider.ANTHROPIC, AiProvider.GEMINI);
		assertThat(chain).extracting(AiRuntimeSettings::apiKey).containsExactly("sk-ant-xyz", "AIza-1");
		assertThat(chain).extracting(AiRuntimeSettings::maxOutputTokens).containsOnly(4000);
	}

	private AiProviderConfigEntity config(AiProvider provider, String apiKey, int priority) {
		AiProviderConfigEntity config = new AiProviderConfigEntity(provider);
		config.replaceApiKey(cipher.encrypt(apiKey), "••••");
		config.update(null, provider.defaultModel(), provider.defaultHeavyModel());
		config.enable(priority);
		return config;
	}
}
