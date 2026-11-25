package com.diegohaefliger.atsresumeoptimizer.diagnostics.application;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class LogFiles {

	private static final String CRASH_LOG = "crash.log";
	private static final int MAX_BYTES_READ = 4 * 1024 * 1024;

	private final Optional<Path> appLog;

	LogFiles(@Value("${logging.file.name:}") String fileName) {
		this.appLog = fileName.isBlank() ? Optional.empty() : Optional.of(Path.of(fileName).toAbsolutePath());
	}

	Optional<Path> appLog() {
		return appLog;
	}

	Optional<Path> folder() {
		return appLog.map(Path::getParent);
	}

	Optional<Path> crashLog() {
		return folder().map(folder -> folder.resolve(CRASH_LOG)).filter(Files::isRegularFile);
	}

	static String tail(Path file, int maxLines) throws IOException {
		if (!Files.isRegularFile(file)) {
			return "";
		}
		List<String> lines = Arrays.asList(readEnd(file).split("\\R", -1));
		return String.join(System.lineSeparator(), lines.subList(Math.max(0, lines.size() - maxLines), lines.size()));
	}

	private static String readEnd(Path file) throws IOException {
		try (SeekableByteChannel channel = Files.newByteChannel(file)) {
			long size = channel.size();
			int length = (int) Math.min(size, MAX_BYTES_READ);
			ByteBuffer buffer = ByteBuffer.allocate(length);
			channel.position(size - length);
			int read = 0;
			while (buffer.hasRemaining() && read >= 0) {
				read = channel.read(buffer);
			}
			return new String(buffer.array(), 0, buffer.position(), StandardCharsets.UTF_8);
		}
	}
}
