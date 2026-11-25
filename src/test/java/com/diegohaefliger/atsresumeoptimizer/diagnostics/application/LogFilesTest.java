package com.diegohaefliger.atsresumeoptimizer.diagnostics.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LogFilesTest {

	@TempDir
	Path folder;

	@Test
	void keepsOnlyTheLastLines() throws Exception {
		Path log = folder.resolve("app.log");
		Files.writeString(log, IntStream.rangeClosed(1, 10).mapToObj(i -> "linha " + i).collect(Collectors.joining("\n")));

		assertThat(LogFiles.tail(log, 3).lines()).containsExactly("linha 8", "linha 9", "linha 10");
	}

	@Test
	void answersEmptyWhenTheLogDoesNotExistYet() throws Exception {
		assertThat(LogFiles.tail(folder.resolve("app.log"), 3)).isEmpty();
	}

	@Test
	void findsTheCrashLogNextToTheAppLog() throws Exception {
		Files.writeString(folder.resolve("crash.log"), "falhou");
		LogFiles logFiles = new LogFiles(folder.resolve("app.log").toString());

		assertThat(logFiles.crashLog()).contains(folder.resolve("crash.log"));
		assertThat(new LogFiles("").folder()).isEmpty();
	}
}
