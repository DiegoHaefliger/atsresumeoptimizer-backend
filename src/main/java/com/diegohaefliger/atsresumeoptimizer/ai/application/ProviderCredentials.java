package com.diegohaefliger.atsresumeoptimizer.ai.application;

import com.diegohaefliger.atsresumeoptimizer.ai.domain.InvalidAiSettingsException;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class ProviderCredentials {

	private static final int API_KEY_HINT_LENGTH = 4;
	private static final String API_KEY_MASK = "••••";
	static final String BASE_URL_CHANGED = "Ao trocar a URL base, informe a chave de API de novo.";

	private final AiProviderConfigRepository providerConfigRepository;
	private final ApiKeyCipher cipher;

	ProviderCredentials(
			AiProviderConfigRepository providerConfigRepository,
			ApiKeyCipher cipher) {
		this.providerConfigRepository = providerConfigRepository;
		this.cipher = cipher;
	}

	/** Chave digitada; senão a salva (só se a URL base não mudou, pra não vazar pra outro servidor). */
	String resolveApiKey(AiProvider provider, String typedApiKey, String typedBaseUrl) {
		if (StringUtils.hasText(typedApiKey)) {
			return typedApiKey.strip();
		}
		Optional<AiProviderConfigEntity> config = providerConfigRepository.findById(provider);
		String baseUrl = baseUrlOf(provider, typedBaseUrl);
		boolean sameBaseUrl = config.map(entity -> entity.apiKeyEncrypted() == null
				|| Objects.equals(entity.baseUrl(), baseUrl)).orElse(true);
		if (!sameBaseUrl) {
			throw new InvalidAiSettingsException(BASE_URL_CHANGED);
		}
		return config.map(AiProviderConfigEntity::apiKeyEncrypted)
				.map(cipher::decrypt)
				.orElse(null);
	}

	String encrypt(String apiKey) {
		return cipher.encrypt(apiKey);
	}

	static String baseUrlOf(AiProvider provider, String baseUrl) {
		return provider.requiresBaseUrl() ? blankToNull(baseUrl) : null;
	}

	static String hint(String apiKey) {
		return apiKey.length() <= API_KEY_HINT_LENGTH
				? API_KEY_MASK
				: API_KEY_MASK + apiKey.substring(apiKey.length() - API_KEY_HINT_LENGTH);
	}

	static String blankToNull(String text) {
		return StringUtils.hasText(text) ? text.strip() : null;
	}
}
