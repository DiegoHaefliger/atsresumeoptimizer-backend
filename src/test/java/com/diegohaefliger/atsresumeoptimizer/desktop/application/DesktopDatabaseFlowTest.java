package com.diegohaefliger.atsresumeoptimizer.desktop.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.servlet.Filter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DesktopDatabaseFlowTest {

	static Path dataDir;
	static EmbeddedPostgres vps;

	@Autowired
	WebApplicationContext context;

	@Autowired
	JdbcTemplate local;

	MockMvc mvc;

	@DynamicPropertySource
	static void desktopDirectories(DynamicPropertyRegistry registry) throws IOException {
		dataDir = Files.createTempDirectory("ats-desktop-db");
		int port = EmbeddedPostgresServerTest.freePort();
		registry.add("app.desktop.data-dir", dataDir::toString);
		registry.add("app.desktop.postgres-port", () -> port);
	}

	@BeforeAll
	static void startVps() throws IOException {
		vps = EmbeddedPostgres.builder().start();
	}

	@AfterAll
	static void stopVps() throws IOException {
		vps.close();
	}

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context)
				.addFilters(context.getBeansOfType(Filter.class).values().toArray(Filter[]::new))
				.build();
	}

	@Test
	@Order(1)
	void startsOnTheLocalDatabase() throws Exception {
		mvc.perform(get("/api/v1/desktop/database"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mode").value("EMBEDDED"));
	}

	@Test
	@Order(2)
	void testsTheConnectionAndSeesAnEmptyDatabase() throws Exception {
		mvc.perform(post("/api/v1/desktop/database/test").contentType(MediaType.APPLICATION_JSON).content(vpsJson(false)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.serverVersion").isNotEmpty())
				.andExpect(jsonPath("$.empty").value(true));
	}

	@Test
	@Order(3)
	void explainsWhenTheServerDoesNotAnswer() throws Exception {
		String unreachable = vpsJson(false).replace("\"port\": " + vps.getPort(), "\"port\": " + EmbeddedPostgresServerTest.freePort());

		mvc.perform(post("/api/v1/desktop/database/test").contentType(MediaType.APPLICATION_JSON).content(unreachable))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("Não deu pra conectar em localhost:")));
	}

	@Test
	@Order(4)
	void copiesTheLocalDataAndSavesTheConnection() throws Exception {
		mvc.perform(put("/api/v1/job-preference").contentType(MediaType.APPLICATION_JSON)
						.content("{\"benefits\": [\"Vale-refeição\"], \"minSalary\": 5000}"))
				.andExpect(status().isOk());
		long sequence = local.queryForObject("SELECT nextval('job_posting_code_seq')", Long.class);

		mvc.perform(put("/api/v1/desktop/database").contentType(MediaType.APPLICATION_JSON).content(vpsJson(true)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.restarting").value(false));

		JdbcTemplate remote = new JdbcTemplate(vps.getPostgresDatabase());
		assertThat(remote.queryForObject("SELECT benefits[1] FROM job_preference", String.class)).isEqualTo("Vale-refeição");
		assertThat(remote.queryForObject("SELECT count(*) FROM prompt_template", Long.class))
				.isEqualTo(local.queryForObject("SELECT count(*) FROM prompt_template", Long.class));
		assertThat(remote.queryForObject("SELECT last_value FROM job_posting_code_seq", Long.class)).isEqualTo(sequence);
		assertThat(new ExternalDatabaseStore(dataDir).load()).isPresent();
	}

	@Test
	@Order(5)
	void refusesToCopyOverExistingData() throws Exception {
		mvc.perform(put("/api/v1/desktop/database").contentType(MediaType.APPLICATION_JSON).content(vpsJson(true)))
				.andExpect(status().isConflict());
	}

	@Test
	@Order(6)
	void goesBackToTheLocalDatabaseRememberingTheExternalOne() throws Exception {
		mvc.perform(delete("/api/v1/desktop/database")).andExpect(status().isOk());

		ExternalDatabaseStore store = new ExternalDatabaseStore(dataDir);
		assertThat(store.load()).isEmpty();
		assertThat(store.previous()).isPresent();
	}

	@Test
	@Order(7)
	void rejectsRequestsAddressedToAnotherHost() throws Exception {
		mvc.perform(get("/api/v1/desktop/database").with(request -> {
			request.setServerName("ataque.exemplo.com");
			return request;
		})).andExpect(status().isForbidden());
	}

	private static String vpsJson(boolean copyData) {
		return """
				{"host": "localhost", "port": %d, "database": "postgres", "username": "postgres", "password": "x",
				 "sslMode": "DISABLE", "copyData": %s}
				""".formatted(vps.getPort(), copyData);
	}
}
