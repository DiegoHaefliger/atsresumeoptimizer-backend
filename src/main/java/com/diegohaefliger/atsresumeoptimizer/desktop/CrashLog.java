package com.diegohaefliger.atsresumeoptimizer.desktop;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.Optional;

// Grava sem Logback: falha antes do Spring subir não chega ao app.log e o launcher do Windows não tem console.
final class CrashLog implements Thread.UncaughtExceptionHandler {

	private static final String LOGS_FOLDER = "logs";
	private static final String FILE_NAME = "crash.log";
	private static final String UNKNOWN_VERSION = "desconhecida";

	private final Path file;

	CrashLog(Path dataDir) {
		this.file = dataDir.resolve(LOGS_FOLDER).resolve(FILE_NAME);
	}

	Path file() {
		return file;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable failure) {
		System.err.print("Exception in thread \"" + thread.getName() + "\" ");
		failure.printStackTrace();
		record(thread, failure);
	}

	private void record(Thread thread, Throwable failure) {
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, entry(thread, failure), StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException | RuntimeException writeFailure) {
			System.err.println("Não deu pra gravar " + file + ": " + writeFailure);
		}
	}

	private static String entry(Thread thread, Throwable failure) {
		StringWriter trace = new StringWriter();
		failure.printStackTrace(new PrintWriter(trace));
		String version = Optional.ofNullable(CrashLog.class.getPackage().getImplementationVersion()).orElse(UNKNOWN_VERSION);
		return "%s | thread %s | app %s | java %s em %s | %s %s %s%n%s%n".formatted(
				OffsetDateTime.now(), thread.getName(), version,
				System.getProperty("java.version"), System.getProperty("java.home"),
				System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"),
				trace);
	}
}
