package com.diegohaefliger.atsresumeoptimizer.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SingleInstanceLockTest {

	@TempDir
	Path dataDir;

	@Test
	void discardsThePortLeftByAPreviousRunSoNobodyOpensAStaleAddress() throws Exception {
		Files.writeString(dataDir.resolve("app.port"), "9999");

		assertThat(SingleInstanceLock.tryAcquire(dataDir)).isPresent();

		assertThat(SingleInstanceLock.publishedPort(dataDir)).isEmpty();
	}

	@Test
	void letsAnotherLaunchFindThePortOnceTheAppIsReady() throws Exception {
		SingleInstanceLock lock = SingleInstanceLock.tryAcquire(dataDir).orElseThrow();

		lock.publishPort(8123, "1.1.14");

		assertThat(SingleInstanceLock.publishedPort(dataDir)).contains(8123);
		assertThat(SingleInstanceLock.publishedVersion(dataDir)).contains("1.1.14");
	}

	@Test
	void stopsAdvertisingThePortWhenTheAppStartsClosing() throws Exception {
		SingleInstanceLock lock = SingleInstanceLock.tryAcquire(dataDir).orElseThrow();
		lock.publishPort(8123, "1.1.14");

		lock.withdrawPort();

		assertThat(SingleInstanceLock.publishedPort(dataDir)).isEmpty();
		assertThat(SingleInstanceLock.publishedVersion(dataDir)).isEmpty();
	}

	@Test
	void anInstanceFromBeforeTheVersionFileAdvertisesNoVersion() throws Exception {
		Files.writeString(dataDir.resolve("app.port"), "8123");

		assertThat(SingleInstanceLock.publishedVersion(dataDir)).isEmpty();
	}

	@Test
	void treatsARestartInsideTheSameProcessAsTheSameInstance() throws Exception {
		assertThat(SingleInstanceLock.tryAcquire(dataDir)).isPresent();

		assertThat(SingleInstanceLock.tryAcquire(dataDir)).isPresent();
	}
}
