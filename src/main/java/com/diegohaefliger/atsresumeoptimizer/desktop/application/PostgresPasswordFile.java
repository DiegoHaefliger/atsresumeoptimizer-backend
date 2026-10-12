package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.HexFormat;

class PostgresPasswordFile {

	private static final int PASSWORD_BYTES = 32;

	private final Path file;
	private final SecureRandom random = new SecureRandom();

	PostgresPasswordFile(Path file) {
		this.file = file;
	}

	Path path() {
		return file;
	}

	boolean exists() {
		return Files.isRegularFile(file);
	}

	String readOrCreate() throws IOException {
		if (exists()) {
			return Files.readString(file, StandardCharsets.UTF_8).strip();
		}
		byte[] bytes = new byte[PASSWORD_BYTES];
		random.nextBytes(bytes);
		String password = HexFormat.of().formatHex(bytes);
		Files.createDirectories(file.toAbsolutePath().getParent());
		Files.writeString(file, password, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
		if (Files.getFileStore(file).supportsFileAttributeView("posix")) {
			Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
		}
		return password;
	}
}
