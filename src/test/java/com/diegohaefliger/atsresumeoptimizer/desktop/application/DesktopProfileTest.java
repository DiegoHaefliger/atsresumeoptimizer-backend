package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("desktop")
@DirtiesContext
class DesktopProfileTest {

	static int postgresPort;

	@Autowired
	JdbcTemplate jdbc;

	@DynamicPropertySource
	static void desktopDataDir(DynamicPropertyRegistry registry) throws IOException {
		Path dataDir = Files.createTempDirectory("ats-desktop");
		postgresPort = EmbeddedPostgresServerTest.freePort();
		registry.add("app.desktop.data-dir", dataDir::toString);
		registry.add("app.desktop.postgres-port", () -> postgresPort);
	}

	@Test
	void startsOnTheEmbeddedPostgresAndRunsTheMigrations() {
		assertThat(jdbc.queryForObject("SELECT inet_server_port()", Integer.class)).isEqualTo(postgresPort);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM databasechangelog", Integer.class)).isPositive();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM resume", Integer.class)).isZero();
	}
}
