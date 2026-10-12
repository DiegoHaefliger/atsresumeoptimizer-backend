package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;

class EmbeddedPostgresServer implements JdbcConnectionDetails, AutoCloseable {

	private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedPostgresServer.class);
	private static final String SUPERUSER = "postgres";
	private static final String DATABASE = "postgres";
	private static final String PASSWORD_AUTH = "scram-sha-256";
	private static final int MAX_CONNECTIONS = 20;

	private final EmbeddedPostgres postgres;
	private final String password;

	EmbeddedPostgresServer(DesktopProperties properties) throws IOException {
		Path dataDirectory = properties.dataDir().resolve("pgdata");
		PostgresPasswordFile passwordFile = new PostgresPasswordFile(properties.dataDir().resolve("postgres-password"));
		if (Files.exists(dataDirectory.resolve("postgresql.conf")) && !passwordFile.exists()) {
			throw new IllegalStateException("O banco em " + dataDirectory + " existe, mas a senha dele sumiu de "
					+ passwordFile.path() + ". Restaure o arquivo do backup.");
		}
		this.password = passwordFile.readOrCreate();
		this.postgres = EmbeddedPostgres.builder()
				.setDataDirectory(dataDirectory)
				.setCleanDataDirectory(false)
				// Quem para o banco é o Spring, depois do pool; o hook do zonky parava antes e travava o shutdown em 30 s.
				.setRegisterShutdownHook(false)
				.setOverrideWorkingDirectory(properties.dataDir().resolve("postgres-bin").toFile())
				.setPort(properties.postgresPort())
				.setPGStartupWait(properties.postgresStartupTimeout())
				// As opções de locale do zonky viram argumentos do initdb; é o único jeito de trocar o "-A trust" fixo dele.
				.setLocaleConfig("auth", PASSWORD_AUTH)
				.setLocaleConfig("pwfile", passwordFile.path().toString())
				.setLocaleConfig("locale", "C")
				.setConnectConfig("password", password)
				// O zonky é feito pra teste e desliga a durabilidade; aqui o banco guarda dado de verdade.
				.setServerConfig("fsync", "on")
				.setServerConfig("synchronous_commit", "on")
				.setServerConfig("listen_addresses", "localhost")
				.setServerConfig("max_connections", Integer.toString(MAX_CONNECTIONS))
				.start();
		LOGGER.info("Postgres embutido na porta {}, dados em {}", postgres.getPort(), dataDirectory);
	}

	@Override
	public String getJdbcUrl() {
		return "jdbc:postgresql://localhost:" + postgres.getPort() + "/" + DATABASE;
	}

	@Override
	public String getUsername() {
		return SUPERUSER;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public void close() throws IOException {
		postgres.close();
	}
}
