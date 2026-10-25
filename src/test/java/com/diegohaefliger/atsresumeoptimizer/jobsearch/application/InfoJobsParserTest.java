package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class InfoJobsParserTest {

	private static String fixture(String name) throws IOException {
		return new ClassPathResource("jobsearch/" + name).getContentAsString(StandardCharsets.UTF_8);
	}

	@Test
	void readsTheCardsOfTheSearchPage() throws IOException {
		List<SourceJob> jobs = InfoJobsParser.parseList(fixture("infojobs-list.html"), "https://infojobs.test");

		assertThat(jobs).hasSize(2);
		SourceJob first = jobs.get(0);
		assertThat(first.externalId()).isEqualTo("dev-java-senior-hibrido-sp-em-sao-paulo__12139852");
		assertThat(first.url()).isEqualTo("https://infojobs.test/vaga-de-dev-java-senior-hibrido-sp-em-sao-paulo__12139852.aspx");
		assertThat(first.title()).isEqualTo("Dev. Java Sênior-Hibrido-SP");
		assertThat(first.company()).isEqualTo("TIVIA TI");
		assertThat(first.location()).isEqualTo("São Paulo - SP");
		assertThat(first.postedOn()).isEqualTo(LocalDate.of(2026, 10, 21));
		assertThat(first.detail()).isNull();
	}

	@Test
	void readsTheJobPostingOfTheDetailPage() throws IOException {
		SourceJobDetail detail = InfoJobsParser.parseDetail(fixture("infojobs-detail.html")).orElseThrow();

		assertThat(detail.description()).contains("Missão:").contains("Java 8/11/17/21");
		assertThat(detail.contractType()).isEqualTo(ContractType.CLT);
		assertThat(detail.postedOn()).isEqualTo(LocalDate.of(2026, 10, 21));
		assertThat(detail.salary().min()).isEqualByComparingTo("10000");
		assertThat(detail.salary().max()).isEqualByComparingTo("12000");
	}

	@Test
	void hasNoDetailWithoutAJobPosting() {
		assertThat(InfoJobsParser.parseDetail("<html><body>nada</body></html>")).isEmpty();
	}
}
