package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import com.diegohaefliger.atsresumeoptimizer.desktop.domain.ExternalDatabase;
import com.diegohaefliger.atsresumeoptimizer.desktop.domain.SslMode;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;
import java.util.Properties;

public class ExternalDatabaseStore {

	private static final String FILE = "database.properties";
	private static final String DISABLED_FILE = "database.properties.disabled";

	private final Path file;
	private final Path disabledFile;

	public ExternalDatabaseStore(Path dataDir) {
		this.file = dataDir.resolve(FILE);
		this.disabledFile = dataDir.resolve(DISABLED_FILE);
	}

	public Optional<ExternalDatabase> load() {
		return read(file);
	}

	public Optional<ExternalDatabase> previous() {
		return read(disabledFile);
	}

	public void save(ExternalDatabase database) {
		Properties properties = new Properties();
		properties.setProperty("host", database.host());
		properties.setProperty("port", Integer.toString(database.port()));
		properties.setProperty("database", database.database());
		properties.setProperty("username", database.username());
		properties.setProperty("password", database.password());
		properties.setProperty("ssl-mode", database.sslMode().name());
		try {
			Files.createDirectories(file.getParent());
			Path temporary = Files.createTempFile(file.getParent(), FILE, ".tmp");
			restrictToOwner(temporary);
			try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
				properties.store(writer, null);
			}
			Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			Files.deleteIfExists(disabledFile);
		} catch (IOException exception) {
			throw new UncheckedIOException("Não deu pra gravar " + file, exception);
		}
	}

	public void disable() {
		try {
			if (Files.exists(file)) {
				Files.move(file, disabledFile, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException exception) {
			throw new UncheckedIOException("Não deu pra desativar " + file, exception);
		}
	}

	private static Optional<ExternalDatabase> read(Path path) {
		if (!Files.isRegularFile(path)) {
			return Optional.empty();
		}
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			properties.load(reader);
		} catch (IOException exception) {
			throw new UncheckedIOException("Não deu pra ler " + path, exception);
		}
		return Optional.of(new ExternalDatabase(
				properties.getProperty("host"),
				Integer.parseInt(properties.getProperty("port")),
				properties.getProperty("database"),
				properties.getProperty("username"),
				properties.getProperty("password"),
				SslMode.valueOf(properties.getProperty("ssl-mode"))));
	}

	private static void restrictToOwner(Path path) throws IOException {
		if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
			Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
		}
	}
}
