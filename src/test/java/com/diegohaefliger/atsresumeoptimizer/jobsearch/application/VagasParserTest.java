package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class VagasParserTest {

	private static String fixture(String name) throws IOException {
		return new ClassPathResource("jobsearch/" + name).getContentAsString(StandardCharsets.UTF_8);
	}

	@Test
	void readsTheCardsOfTheSearchPage() throws IOException {
		List<SourceJob> jobs = VagasParser.parseList(fixture("vagas-list.html"), "https://vagas.test");

		assertThat(jobs).hasSize(2);
		SourceJob first = jobs.get(0);
		assertThat(first.externalId()).isEqualTo("2836582/desenvolvedor-java-sr");
		assertThat(first.url()).isEqualTo("https://vagas.test/vagas/v2836582/desenvolvedor-java-sr");
		assertThat(first.title()).isEqualTo("Desenvolvedor Java Sr");
		assertThat(first.company()).isEqualTo("Confidencial");
		assertThat(first.location()).isEqualTo("100% Home Office");
		assertThat(first.postedOn()).isEqualTo(LocalDate.of(2026, 9, 24));
	}

	@Test
	void readsTheDescriptionAndBenefitsOfTheDetailPage() throws IOException {
		SourceJobDetail detail = VagasParser.parseDetail(fixture("vagas-detail.html")).orElseThrow();

		assertThat(detail.description()).contains("A Synchro está em busca").contains("Benefícios: Assistência médica");
		assertThat(detail.postedOn()).isEqualTo(LocalDate.of(2026, 8, 14));
	}

	@Test
	void hasNoDetailWithoutAJobPosting() {
		assertThat(VagasParser.parseDetail("<html><body>nada</body></html>")).isEmpty();
	}
}
