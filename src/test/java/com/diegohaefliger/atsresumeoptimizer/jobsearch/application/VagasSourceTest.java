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

class VagasSourceTest {

	private final JobSearchProperties properties = new JobSearchProperties("https://linkedin.test", "test-agent",
			Duration.ZERO, Duration.ZERO, Duration.ofSeconds(1), "Brazil", "https://gupy.test", "https://programathor.test",
			"https://infojobs.test", "https://vagas.test", "https://api.remotar.test", "https://remotar.test");

	private MockRestServiceServer server;
	private VagasSource source;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		source = new VagasSource(properties, builder);
	}

	private static String fixture(String name) throws IOException {
		return new ClassPathResource("jobsearch/" + name).getContentAsString(StandardCharsets.UTF_8);
	}

	private static SourceQuery query(String keyword, int start) {
		return new SourceQuery(keyword, "Brazil", PostedWithin.ANY, List.of(), List.of(), List.of(), start);
	}

	@Test
	void searchesBySlugOfTheKeyword() throws IOException {
		server.expect(requestTo("https://vagas.test/vagas-de-desenvolvedor-java"))
				.andRespond(withSuccess(fixture("vagas-list.html"), MediaType.TEXT_HTML));

		assertThat(source.search(query("Desenvolvedor Java", 0))).hasSize(2);
		server.verify();
	}

	@Test
	void servesOnlyTheFirstPage() {
		assertThat(source.search(query("java", 8))).isEmpty();
		server.verify();
	}

	@Test
	void readsTheJobDetail() throws IOException {
		server.expect(requestTo("https://vagas.test/vagas/v123/dev-java"))
				.andRespond(withSuccess(fixture("vagas-detail.html"), MediaType.TEXT_HTML));

		assertThat(source.detail("123/dev-java")).isPresent();
		server.verify();
	}

	@Test
	void reportsAFriendlyErrorWhenTheSiteFails() {
		server.expect(requestTo("https://vagas.test/vagas-de-desenvolvedor-java")).andRespond(withServerError());

		assertThatThrownBy(() -> source.search(query("Desenvolvedor Java", 0))).isInstanceOf(JobSourceException.class)
				.hasMessageContaining("Vagas.com");
	}

	@Test
	void recognizesItsOwnJobLinks() {
		assertThat(source.externalIdOf("https://www.vagas.com.br/vagas/v2830602/desenvolvedor-java")).contains("2830602/desenvolvedor-java");
		assertThat(source.externalIdOf("https://example.com/vaga/1")).isEmpty();
	}
}
