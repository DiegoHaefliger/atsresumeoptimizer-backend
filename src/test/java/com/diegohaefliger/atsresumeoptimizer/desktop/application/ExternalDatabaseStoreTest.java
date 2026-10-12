package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.desktop.domain.ExternalDatabase;
import com.diegohaefliger.atsresumeoptimizer.desktop.domain.SslMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExternalDatabaseStoreTest {

	private static final ExternalDatabase VPS = new ExternalDatabase("vps.exemplo.com", 5432, "ats", "ats", "s3nh@#=!", SslMode.VERIFY_FULL);

	@TempDir
	Path dataDir;

	@Test
	void usesTheEmbeddedDatabaseUntilSomethingIsSaved() {
		assertThat(new ExternalDatabaseStore(dataDir).load()).isEmpty();
	}

	@Test
	void keepsTheConnectionIncludingPasswordsWithSymbols() {
		ExternalDatabaseStore store = new ExternalDatabaseStore(dataDir);

		store.save(VPS);

		assertThat(new ExternalDatabaseStore(dataDir).load()).contains(VPS);
	}

	@Test
	void writesTheFileReadableOnlyByTheOwner() throws Exception {
		new ExternalDatabaseStore(dataDir).save(VPS);

		Path file = dataDir.resolve("database.properties");
		assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(file))).isEqualTo("rw-------");
	}

	@Test
	void goingBackToTheLocalDatabaseRemembersTheLastExternalOne() {
		ExternalDatabaseStore store = new ExternalDatabaseStore(dataDir);
		store.save(VPS);

		store.disable();

		assertThat(store.load()).isEmpty();
		assertThat(store.previous()).contains(VPS);
	}

	@Test
	void savingAgainForgetsTheDisabledCopy() {
		ExternalDatabaseStore store = new ExternalDatabaseStore(dataDir);
		store.save(VPS);
		store.disable();

		store.save(VPS);

		assertThat(store.previous()).isEmpty();
		assertThat(store.load()).contains(VPS);
	}
}
