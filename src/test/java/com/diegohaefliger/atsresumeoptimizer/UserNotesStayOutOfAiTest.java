package com.diegohaefliger.atsresumeoptimizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserNotesStayOutOfAiTest {

	private static final Path MAIN_SOURCES = Path.of("src/main/java/com/diegohaefliger/atsresumeoptimizer");

	@ParameterizedTest
	@ValueSource(strings = {"jobnote", "personalnote"})
	void noOtherModuleReadsTheNotes(String module) throws IOException {
		Path moduleDirectory = MAIN_SOURCES.resolve(module);
		String packageName = "atsresumeoptimizer." + module;
		List<Path> readers;
		try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
			readers = files.filter(path -> path.toString().endsWith(".java"))
					.filter(path -> !path.startsWith(moduleDirectory))
					.filter(path -> mentions(path, packageName))
					.toList();
		}

		assertThat(readers).as("módulos que importam %s", module).isEmpty();
	}

	private static boolean mentions(Path path, String packageName) {
		try {
			return Files.readString(path).contains(packageName);
		} catch (IOException e) {
			throw new IllegalStateException(path.toString(), e);
		}
	}
}
