package com.diegohaefliger.atsresumeoptimizer.ai.application;

import com.diegohaefliger.atsresumeoptimizer.ai.domain.AiSettingsSecretMissingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Segredo fica fora do banco (variável ou arquivo gerado) pra chave e dado cifrado não morarem no mesmo lugar. */
@Component
class SettingsSecretStore {

	private static final Logger LOGGER = LoggerFactory.getLogger(SettingsSecretStore.class);
	private static final int SECRET_BYTES = 48;

	private final String configuredSecret;
	private final Path secretFile;
	private final SecureRandom random = new SecureRandom();

	SettingsSecretStore(AiProperties properties) {
		this.configuredSecret = properties.settingsSecret();
		this.secretFile = properties.settingsSecretFile() == null || properties.settingsSecretFile().isBlank()
				? null
				: Path.of(properties.settingsSecretFile());
	}

	Optional<String> current() {
		if (configuredSecret != null && !configuredSecret.isBlank()) {
			return Optional.of(configuredSecret);
		}
		if (secretFile == null || !Files.isRegularFile(secretFile)) {
			return Optional.empty();
		}
		try {
			return Optional.of(Files.readString(secretFile, StandardCharsets.UTF_8).strip()).filter(secret -> !secret.isEmpty());
		} catch (IOException exception) {
			throw new IllegalStateException("Não deu pra ler o segredo das chaves de IA em " + secretFile, exception);
		}
	}

	synchronized String currentOrCreate() {
		return current().orElseGet(this::create);
	}

	boolean canEncrypt() {
		return current().isPresent() || secretFile != null;
	}

	private String create() {
		if (secretFile == null) {
			throw new AiSettingsSecretMissingException();
		}
		byte[] bytes = new byte[SECRET_BYTES];
		random.nextBytes(bytes);
		String secret = Base64.getEncoder().encodeToString(bytes);
		try {
			Files.createDirectories(secretFile.toAbsolutePath().getParent());
			Files.writeString(secretFile, secret, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
			restrictToOwner();
		} catch (FileAlreadyExistsException concurrentlyCreated) {
			return current().orElseThrow(AiSettingsSecretMissingException::new);
		} catch (IOException exception) {
			throw new IllegalStateException("Não deu pra gravar o segredo das chaves de IA em " + secretFile, exception);
		}
		LOGGER.info("Segredo das chaves de IA gerado em {}. Faça backup: sem ele as chaves salvas não abrem.", secretFile);
		return secret;
	}

	private void restrictToOwner() throws IOException {
		if (Files.getFileStore(secretFile).supportsFileAttributeView("posix")) {
			Files.setPosixFilePermissions(secretFile, PosixFilePermissions.fromString("rw-------"));
		}
	}
}
