package com.diegohaefliger.atsresumeoptimizer.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SingleInstanceLockTest {

	@TempDir
	Path dataDir;

	@Test
	void discardsThePortLeftByAPreviousRunSoNobodyOpensAStaleAddress() throws Exception {
		Files.writeString(dataDir.resolve("app.port"), "9999");

		assertThat(SingleInstanceLock.tryAcquire(dataDir)).isPresent();

		assertThat(SingleInstanceLock.waitForPublishedPort(dataDir, Duration.ZERO)).isEmpty();
	}

	@Test
	void letsAnotherLaunchFindThePortOnceTheAppIsReady() throws Exception {
		SingleInstanceLock lock = SingleInstanceLock.tryAcquire(dataDir).orElseThrow();

		lock.publishPort(8123);

		assertThat(SingleInstanceLock.waitForPublishedPort(dataDir, Duration.ofSeconds(1))).contains(8123);
	}

	@Test
	void treatsARestartInsideTheSameProcessAsTheSameInstance() throws Exception {
		assertThat(SingleInstanceLock.tryAcquire(dataDir)).isPresent();

		assertThat(SingleInstanceLock.tryAcquire(dataDir)).isPresent();
	}
}
