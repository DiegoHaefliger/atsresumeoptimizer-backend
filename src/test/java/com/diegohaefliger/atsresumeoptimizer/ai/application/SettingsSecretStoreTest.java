package com.diegohaefliger.atsresumeoptimizer.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsSecretStoreTest {

	@Test
	void prefersTheSecretFromTheEnvironmentAndNeverWritesTheFile(@TempDir Path directory) {
		Path secretFile = directory.resolve("ai-settings-secret");
		SettingsSecretStore store = new SettingsSecretStore(ApiKeyCipherTest.properties("do-ambiente", secretFile));

		assertThat(store.currentOrCreate()).isEqualTo("do-ambiente");
		assertThat(secretFile).doesNotExist();
	}

	@Test
	void generatesTheFileOnceReadableOnlyByTheOwnerAndReusesIt(@TempDir Path directory) throws Exception {
		Path secretFile = directory.resolve("ai-settings-secret");
		SettingsSecretStore store = new SettingsSecretStore(ApiKeyCipherTest.properties(null, secretFile));
		assertThat(store.current()).isEmpty();
		assertThat(store.canEncrypt()).isTrue();

		String generated = store.currentOrCreate();

		assertThat(generated).hasSizeGreaterThan(40);
		assertThat(store.currentOrCreate()).isEqualTo(generated);
		assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(secretFile))).isEqualTo("rw-------");
	}

	@Test
	void cannotEncryptWithoutSecretAndWithoutFile() {
		assertThat(new SettingsSecretStore(ApiKeyCipherTest.properties(null)).canEncrypt()).isFalse();
	}
}
