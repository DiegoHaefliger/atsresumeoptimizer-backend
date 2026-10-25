package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;

class ProgramathorParserTest {

	private static String fixture(String name) throws IOException {
		return new ClassPathResource("jobsearch/" + name).getContentAsString(StandardCharsets.UTF_8);
	}

	@Test
	void readsTheCardsAndIgnoresLinksThatAreNotJobs() throws IOException {
		List<SourceJob> jobs = ProgramathorParser.parseList(fixture("programathor-list.html"), "https://programathor.test");

		assertThat(jobs).hasSize(2);
		SourceJob first = jobs.get(0);
		assertThat(first.externalId()).isEqualTo("33724-desenvolvedor-a-mobile-flutter-senior");
		assertThat(first.url()).isEqualTo("https://programathor.test/jobs/33724-desenvolvedor-a-mobile-flutter-senior");
		assertThat(first.title()).isEqualTo("Desenvolvedor(a) Mobile Flutter - Sênior");
		assertThat(first.company()).isEqualTo("Ubuntu Future Labs");
		assertThat(first.location()).isEqualTo("Remoto");
		assertThat(first.detail()).isNull();
	}

	@Test
	void readsTheDetailFromTheStructuredData() throws IOException {
		SourceJobDetail detail = ProgramathorParser.parseDetail(fixture("programathor-detail.html")).orElseThrow();

		assertThat(detail.description()).contains("Atuar com Flutter.").contains("• Dart").contains("• Firebase");
		assertThat(detail.contractType()).isEqualTo(ContractType.PJ);
		assertThat(detail.workModel()).isEqualTo(WorkModel.REMOTE);
		assertThat(detail.postedOn()).isEqualTo(LocalDate.of(2026, 8, 19));
	}

	@Test
	void returnsNothingWhenThePageHasNoJobPosting() {
		assertThat(ProgramathorParser.parseDetail("<html><body>oi</body></html>")).isEmpty();
	}
}
