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

class GupySourceTest {

	private static final String SEARCH = "https://gupy.test/job-search/term=java&city=Curitiba";

	private final JobSearchProperties properties = new JobSearchProperties("https://linkedin.test", "test-agent",
			Duration.ZERO, Duration.ZERO, Duration.ofSeconds(1), "Brazil", "https://gupy.test", "https://programathor.test", "https://infojobs.test", "https://vagas.test", "https://api.remotar.test", "https://remotar.test");

	private MockRestServiceServer server;
	private GupySource source;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		source = new GupySource(properties, builder);
	}

	private static SourceQuery query(int start) {
		return new SourceQuery("java", "Curitiba", PostedWithin.ANY, List.of(), List.of(), List.of(), start);
	}

	@Test
	void readsTheJobsFromTheSearchPage() throws IOException {
		server.expect(requestTo(SEARCH)).andRespond(withSuccess(
				new ClassPathResource("jobsearch/gupy-search.html").getContentAsString(StandardCharsets.UTF_8),
				MediaType.TEXT_HTML));

		assertThat(source.search(query(0))).hasSize(2);
		server.verify();
	}

	@Test
	void doesNotAskForASecondPageBecauseThePortalOnlyServesTheFirst() {
		assertThat(source.search(query(12))).isEmpty();
		server.verify();
	}

	@Test
	void reportsAnErrorFromThePortal() {
		server.expect(requestTo(SEARCH)).andRespond(withServerError());

		assertThatThrownBy(() -> source.search(query(0))).isInstanceOf(JobSourceException.class)
				.hasMessageContaining("Gupy");
	}

	@Test
	void hidesTheTechnicalCauseWhenThePortalIsUnreachable() {
		server.expect(requestTo(SEARCH)).andRespond(request -> {
			throw new java.io.IOException("connection reset");
		});

		assertThatThrownBy(() -> source.search(query(0))).isInstanceOf(JobSourceException.class)
				.hasMessage("Não deu pra acessar a Gupy. O site pode estar fora do ar ou sem conexão.");
	}
}
