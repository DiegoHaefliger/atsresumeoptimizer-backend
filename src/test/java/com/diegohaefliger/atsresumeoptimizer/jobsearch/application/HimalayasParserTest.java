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

class HimalayasParserTest {

	private static String fixture() throws IOException {
		return new ClassPathResource("jobsearch/himalayas-search.json").getContentAsString(StandardCharsets.UTF_8);
	}

	@Test
	void readsTheJobsKeyedByGuid() throws IOException {
		List<SourceJob> jobs = HimalayasParser.parse(fixture());

		assertThat(jobs).extracting(SourceJob::externalId).containsExactly(
				"https://himalayas.app/companies/alien-intellect/jobs/senior-software-engineer-8699633569",
				"https://himalayas.app/companies/talentcross/jobs/java-intern-1");
		SourceJob first = jobs.get(0);
		assertThat(first.company()).isEqualTo("Alien Intellect");
		assertThat(first.location()).isEqualTo("Brazil, Argentina");
		assertThat(first.postedOn()).isEqualTo(LocalDate.of(2026, 10, 26));
		assertThat(first.detail().description()).contains("Build Java services.").contains("• Spring Boot");
		assertThat(first.detail().workModel()).isEqualTo(WorkModel.REMOTE);
		assertThat(first.detail().seniorityLevel()).isEqualTo("Senior");
	}

	@Test
	void fallsBackToTheCompanySlugAndMapsInternships() throws IOException {
		SourceJob intern = HimalayasParser.parse(fixture()).get(1);

		assertThat(intern.company()).isEqualTo("talentcross");
		assertThat(intern.location()).isEqualTo("Remoto");
		assertThat(intern.detail().contractType()).isEqualTo(ContractType.INTERNSHIP);
	}

	@Test
	void ignoresBrokenPayloads() {
		assertThat(HimalayasParser.parse("não é json")).isEmpty();
		assertThat(HimalayasParser.parse("{\"jobs\":null}")).isEmpty();
	}
}
