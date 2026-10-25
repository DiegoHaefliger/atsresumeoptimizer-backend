package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class RemotarParserTest {

	private static String fixture() throws IOException {
		return new ClassPathResource("jobsearch/remotar-jobs.json").getContentAsString(StandardCharsets.UTF_8);
	}

	@Test
	void readsTheActiveJobsWithTheDescriptionInline() throws IOException {
		List<SourceJob> jobs = RemotarParser.parse(fixture(), "https://remotar.test");

		assertThat(jobs).extracting(SourceJob::externalId).containsExactly("168194", "168193", "999002");
		SourceJob first = jobs.get(0);
		assertThat(first.url()).isEqualTo("https://remotar.test/job/168194");
		assertThat(first.company()).isEqualTo("Grupo FCamara");
		assertThat(first.location()).isEqualTo("Remoto");
		assertThat(first.postedOn()).isEqualTo(LocalDate.of(2026, 10, 23));
		assertThat(first.detail().description()).isNotBlank();
		assertThat(first.detail().workModel()).isEqualTo(WorkModel.REMOTE);
	}

	@Test
	void readsLocationHybridModelAndBenefits() throws IOException {
		SourceJob hybrid = RemotarParser.parse(fixture(), "https://remotar.test").get(2);

		assertThat(hybrid.location()).isEqualTo("Curitiba, PR");
		assertThat(hybrid.detail().workModel()).isEqualTo(WorkModel.HYBRID);
		assertThat(hybrid.detail().description()).endsWith("Benefícios: Vale-refeição, Plano de saúde");
	}

	@Test
	void ignoresExpiredJobsAndBrokenPayloads() throws IOException {
		assertThat(RemotarParser.parse(fixture(), "https://remotar.test")).extracting(SourceJob::title)
				.doesNotContain("Vaga encerrada");
		assertThat(RemotarParser.parse("não é json", "https://remotar.test")).isEmpty();
		assertThat(RemotarParser.parse("{\"data\":null}", "https://remotar.test")).isEmpty();
	}
}
