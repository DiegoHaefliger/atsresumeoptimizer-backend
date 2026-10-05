package com.diegohaefliger.atsresumeoptimizer.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.diegohaefliger.atsresumeoptimizer.ai.domain.AiSettingsSecretMissingException;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApiKeyCipherTest {

	@Test
	void encryptsWithARandomIvAndDecryptsBackToTheOriginalKey() {
		ApiKeyCipher cipher = cipher("segredo-de-teste");

		String first = cipher.encrypt("sk-abc123");
		String second = cipher.encrypt("sk-abc123");

		assertThat(first).isNotEqualTo(second).doesNotContain("sk-abc123");
		assertThat(cipher.decrypt(first)).isEqualTo("sk-abc123");
	}

	@Test
	void generatesTheSecretOnTheFirstSaveWhenNoneIsConfigured(@TempDir Path directory) {
		Path secretFile = directory.resolve("segredos/ai-settings-secret");
		ApiKeyCipher cipher = new ApiKeyCipher(new SettingsSecretStore(properties(" ", secretFile)));

		String encrypted = cipher.encrypt("sk-abc123");

		assertThat(secretFile).exists();
		ApiKeyCipher afterRestart = new ApiKeyCipher(new SettingsSecretStore(properties(null, secretFile)));
		assertThat(afterRestart.decrypt(encrypted)).isEqualTo("sk-abc123");
	}

	@Test
	void refusesToStoreAKeyWithoutSecretAndWithoutAPlaceToGenerateIt() {
		assertThatThrownBy(() -> cipher(" ").encrypt("sk-abc123")).isInstanceOf(AiSettingsSecretMissingException.class);
	}

	@Test
	void doesNotOpenAKeyEncryptedWithAnotherSecret() {
		String encrypted = cipher("segredo-a").encrypt("sk-abc123");

		assertThatThrownBy(() -> cipher("segredo-b").decrypt(encrypted)).isInstanceOf(IllegalStateException.class);
	}

	static AiProperties properties(String secret) {
		return properties(secret, null);
	}

	static AiProperties properties(String secret, Path secretFile) {
		return new AiProperties(0.1, 2000, Duration.ofSeconds(60), null, secret,
				secretFile != null ? secretFile.toString() : null);
	}

	private static ApiKeyCipher cipher(String secret) {
		return new ApiKeyCipher(new SettingsSecretStore(properties(secret)));
	}
}
