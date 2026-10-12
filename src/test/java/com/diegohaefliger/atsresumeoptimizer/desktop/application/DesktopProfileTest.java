package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("desktop")
@DirtiesContext
class DesktopProfileTest {

	private static final String INDEX_HTML = "<!doctype html><div id=\"root\"></div>";
	private static final String APP_JS = "console.log('app')";

	static int postgresPort;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	WebApplicationContext context;

	MockMvc mvc;

	@DynamicPropertySource
	static void desktopDirectories(DynamicPropertyRegistry registry) throws IOException {
		Path dataDir = Files.createTempDirectory("ats-desktop");
		Path frontendDir = Files.createTempDirectory("ats-frontend");
		Files.writeString(frontendDir.resolve("index.html"), INDEX_HTML);
		Files.createDirectories(frontendDir.resolve("assets"));
		Files.writeString(frontendDir.resolve("assets/app.js"), APP_JS);
		postgresPort = EmbeddedPostgresServerTest.freePort();
		registry.add("app.desktop.data-dir", dataDir::toString);
		registry.add("app.desktop.postgres-port", () -> postgresPort);
		registry.add("app.desktop.frontend-location", () -> frontendDir.toUri().toString());
	}

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test
	void startsOnTheEmbeddedPostgresAndRunsTheMigrations() {
		assertThat(jdbc.queryForObject("SELECT inet_server_port()", Integer.class)).isEqualTo(postgresPort);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM databasechangelog", Integer.class)).isPositive();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM resume", Integer.class)).isZero();
	}

	@Test
	void servesTheFrontendAtTheRoot() throws Exception {
		mvc.perform(get("/")).andExpect(forwardedUrl("/index.html"));
		mvc.perform(get("/index.html")).andExpect(status().isOk()).andExpect(content().string(INDEX_HTML));
		mvc.perform(get("/assets/app.js")).andExpect(status().isOk()).andExpect(content().string(APP_JS));
	}

	@Test
	void answersClientRoutesWithTheIndexSoReloadWorks() throws Exception {
		mvc.perform(get("/analyses/123")).andExpect(status().isOk()).andExpect(content().string(INDEX_HTML));
	}

	@Test
	void keepsTheApiOutOfTheFallback() throws Exception {
		mvc.perform(get("/api/v1/resumes")).andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
		mvc.perform(get("/api/v1/nao-existe")).andExpect(status().isNotFound());
	}
}
