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

class GupyParserTest {

	private static String fixture() throws IOException {
		return new ClassPathResource("jobsearch/gupy-search.html").getContentAsString(StandardCharsets.UTF_8);
	}

	@Test
	void readsTheJobsAndSkipsTheOnesWithoutTitle() throws IOException {
		List<SourceJob> jobs = GupyParser.parse(fixture());

		assertThat(jobs).hasSize(2);
		SourceJob first = jobs.get(0);
		assertThat(first.externalId()).isEqualTo("12702621");
		assertThat(first.title()).isEqualTo("DESENVOLVEDOR BACK END PL - Java");
		assertThat(first.company()).isEqualTo("Stefanini Group");
		assertThat(first.location()).isEqualTo("Remoto");
		assertThat(first.postedOn()).isEqualTo(LocalDate.of(2026, 10, 23));
		assertThat(first.url()).startsWith("https://stefanini.gupy.io/job/");
	}

	@Test
	void carriesTheDescriptionAndStructuredFieldsSoNoDetailCallIsNeeded() throws IOException {
		List<SourceJob> jobs = GupyParser.parse(fixture());

		SourceJobDetail remote = jobs.get(0).detail();
		assertThat(remote.description()).contains("Spring Boot");
		assertThat(remote.workModel()).isEqualTo(WorkModel.REMOTE);
		assertThat(remote.contractType()).isEqualTo(ContractType.CLT);
		SourceJobDetail hybrid = jobs.get(1).detail();
		assertThat(hybrid.workModel()).isEqualTo(WorkModel.HYBRID);
		assertThat(hybrid.contractType()).isEqualTo(ContractType.PJ);
		assertThat(jobs.get(1).location()).isEqualTo("Curitiba, Paraná");
	}

	@Test
	void turnsHtmlDescriptionsIntoPlainText() throws IOException {
		SourceJobDetail detail = GupyParser.parse(fixture()).get(1).detail();

		assertThat(detail.description()).contains("Atuar com Java.").contains("• Spring").doesNotContain("<");
	}

	@Test
	void returnsNothingWhenThePageHasNoData() {
		assertThat(GupyParser.parse("<html><body>oi</body></html>")).isEmpty();
		assertThat(GupyParser.parse("<html><script id=\"__NEXT_DATA__\">não é json</script></html>")).isEmpty();
	}
}
