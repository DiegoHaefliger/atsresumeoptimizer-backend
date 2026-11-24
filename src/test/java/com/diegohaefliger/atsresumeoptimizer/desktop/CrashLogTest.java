package com.diegohaefliger.atsresumeoptimizer.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CrashLogTest {

	@TempDir
	Path dataDir;

	@Test
	void keepsTheStackTraceOfAFailureThatHappensBeforeLoggingIsConfigured() throws Exception {
		CrashLog crashLog = new CrashLog(dataDir);
		Throwable failure = new IllegalStateException("janela não abriu",
				new ClassNotFoundException("com.sun.java.accessibility.AccessBridge"));

		crashLog.uncaughtException(new Thread("main"), failure);

		String content = Files.readString(dataDir.resolve("logs").resolve("crash.log"));
		assertThat(content)
				.contains("thread main")
				.contains("java " + System.getProperty("java.version"))
				.contains("IllegalStateException: janela não abriu")
				.contains("Caused by: java.lang.ClassNotFoundException: com.sun.java.accessibility.AccessBridge");
	}

	@Test
	void keepsEveryFailureInsteadOfOverwritingTheLastOne() throws Exception {
		CrashLog crashLog = new CrashLog(dataDir);

		crashLog.uncaughtException(new Thread("main"), new IllegalStateException("primeira"));
		crashLog.uncaughtException(new Thread("AWT-EventQueue-0"), new IllegalStateException("segunda"));

		assertThat(Files.readString(crashLog.file())).contains("primeira").contains("segunda");
	}
}
