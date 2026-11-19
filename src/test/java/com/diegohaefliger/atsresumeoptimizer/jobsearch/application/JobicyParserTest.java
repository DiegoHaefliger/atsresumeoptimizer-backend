package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class JobicyParserTest {

	private static String fixture() throws IOException {
		return new ClassPathResource("jobsearch/jobicy-jobs.json").getContentAsString(StandardCharsets.UTF_8);
	}

	@Test
	void readsTheJobsWithTheDescriptionInline() throws IOException {
		List<SourceJob> jobs = JobicyParser.parse(fixture());

		assertThat(jobs).extracting(SourceJob::externalId).containsExactly("163199", "163100");
		SourceJob first = jobs.get(0);
		assertThat(first.url()).isEqualTo("https://jobicy.com/jobs/163199-implementation-architect");
		assertThat(first.company()).isEqualTo("Dropbox");
		assertThat(first.location()).isEqualTo("Canada");
		assertThat(first.postedOn()).isEqualTo(LocalDate.of(2026, 11, 18));
		assertThat(first.detail().description()).contains("customers").contains("• Java");
		assertThat(first.detail().workModel()).isEqualTo(WorkModel.REMOTE);
		assertThat(first.detail().seniorityLevel()).isEqualTo("Director");
		assertThat(first.detail().salary()).isEqualTo(SalaryRange.NONE);
	}

	@Test
	void mapsContractAndFillsAnEmptyDescription() throws IOException {
		SourceJobDetail detail = JobicyParser.parse(fixture()).get(1).detail();

		assertThat(detail.contractType()).isEqualTo(ContractType.PJ);
		assertThat(detail.description()).isEqualTo("Sem descrição.");
	}

	@Test
	void ignoresBrokenPayloads() {
		assertThat(JobicyParser.parse("não é json")).isEmpty();
		assertThat(JobicyParser.parse("{\"jobs\":null}")).isEmpty();
	}
}
