package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import com.diegohaefliger.atsresumeoptimizer.preference.PostedWithin;

class RemotarSourceTest {

	private final JobSearchProperties properties = new JobSearchProperties("https://linkedin.test", "test-agent",
			Duration.ZERO, Duration.ZERO, Duration.ofSeconds(1), "Brazil", "https://gupy.test", "https://programathor.test",
			"https://infojobs.test", "https://vagas.test", "https://api.remotar.test", "https://remotar.test");

	private MockRestServiceServer server;
	private RemotarSource source;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		source = new RemotarSource(properties, builder);
	}

	private static String fixture() throws IOException {
		return new ClassPathResource("jobsearch/remotar-jobs.json").getContentAsString(StandardCharsets.UTF_8);
	}

	private static SourceQuery query(String keyword, int start) {
		return new SourceQuery(keyword, "Brazil", PostedWithin.ANY, List.of(), List.of(), List.of(), start);
	}

	@Test
	void searchesByTheKeywordAndPage() throws IOException {
		server.expect(requestTo("https://api.remotar.test/jobs?search=Desenvolvedor%20Java&page=1"))
				.andRespond(withSuccess(fixture(), MediaType.APPLICATION_JSON));

		assertThat(source.search(query("Desenvolvedor Java", 0))).hasSize(3);
		server.verify();
	}

	@Test
	void asksForTheRightPage() {
		server.expect(requestTo("https://api.remotar.test/jobs?search=java&page=3"))
				.andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));

		assertThat(source.search(query("java", 100))).isEmpty();
		server.verify();
	}

	@Test
	void reportsAFriendlyErrorWhenTheSiteFails() {
		server.expect(requestTo("https://api.remotar.test/jobs?search=java&page=1")).andRespond(withServerError());

		assertThatThrownBy(() -> source.search(query("java", 0))).isInstanceOf(JobSourceException.class)
				.hasMessageContaining("Remotar");
	}

	@Test
	void recognizesItsOwnJobLinks() {
		assertThat(source.externalIdOf("https://remotar.com.br/job/168194")).contains("168194");
		assertThat(source.externalIdOf("https://example.com/job/1")).isEmpty();
	}
}
