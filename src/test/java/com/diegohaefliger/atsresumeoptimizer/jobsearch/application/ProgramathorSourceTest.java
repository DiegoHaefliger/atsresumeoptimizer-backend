package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import com.diegohaefliger.atsresumeoptimizer.preference.PostedWithin;

class ProgramathorSourceTest {

	private final JobSearchProperties properties = new JobSearchProperties("https://linkedin.test", "test-agent",
			Duration.ZERO, Duration.ZERO, Duration.ofSeconds(1), "Brazil", "https://gupy.test", "https://programathor.test", "https://infojobs.test", "https://vagas.test", "https://api.remotar.test", "https://remotar.test");

	private MockRestServiceServer server;
	private ProgramathorSource source;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		source = new ProgramathorSource(properties, builder);
	}

	private static String fixture(String name) throws IOException {
		return new ClassPathResource("jobsearch/" + name).getContentAsString(StandardCharsets.UTF_8);
	}

	private static SourceQuery query(String keyword, int start) {
		return new SourceQuery(keyword, "Brazil", PostedWithin.ANY, List.of(), List.of(), List.of(), start);
	}

	@Test
	void searchesByTheTechnologySlug() throws IOException {
		server.expect(requestTo("https://programathor.test/jobs-java?page=1"))
				.andRespond(withSuccess(fixture("programathor-list.html"), MediaType.TEXT_HTML));

		assertThat(source.search(query("Desenvolvedor Java", 0))).hasSize(2);
		server.verify();
	}

	@Test
	void asksForTheRightPage() {
		server.expect(requestTo("https://programathor.test/jobs-java?page=3")).andRespond(withSuccess("", MediaType.TEXT_HTML));

		assertThat(source.search(query("java", 60))).isEmpty();
		server.verify();
	}

	@Test
	void treatsAnUnknownTechnologyAsNoResults() {
		server.expect(requestTo("https://programathor.test/jobs-xyz?page=1")).andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThat(source.search(query("xyz", 0))).isEmpty();
		server.verify();
	}

	@Test
	void doesNotCallTheSiteWhenThereIsNoTechnologyInTheKeyword() {
		assertThat(source.search(query("Desenvolvedor Backend", 0))).isEmpty();
		server.verify();
	}

	@Test
	void readsTheJobDetailFromItsPage() throws IOException {
		server.expect(requestTo("https://programathor.test/jobs/33724-flutter"))
				.andRespond(withSuccess(fixture("programathor-detail.html"), MediaType.TEXT_HTML));

		assertThat(source.detail("33724-flutter")).isPresent();
		server.verify();
	}
}
