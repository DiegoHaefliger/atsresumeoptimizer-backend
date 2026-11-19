package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class GetOnBoardParserTest {

	private static String fixture() throws IOException {
		return new ClassPathResource("jobsearch/getonboard-search.json").getContentAsString(StandardCharsets.UTF_8);
	}

	@Test
	void readsTheJobsWithTheCompanyExpanded() throws IOException {
		List<SourceJob> jobs = GetOnBoardParser.parse(fixture());

		assertThat(jobs).extracting(SourceJob::externalId).containsExactly("java-backend-developer-arkho-remote",
				"java-hibrido-acme");
		SourceJob first = jobs.get(0);
		assertThat(first.url()).isEqualTo("https://www.getonbrd.com/jobs/java-backend-developer-arkho-remote");
		assertThat(first.company()).isEqualTo("ARKHO");
		assertThat(first.location()).isEqualTo("Brasil, Chile");
		assertThat(first.postedOn()).isEqualTo(LocalDate.of(2026, 11, 18));
		assertThat(first.detail().workModel()).isEqualTo(WorkModel.REMOTE);
	}

	@Test
	void joinsTheDescriptionSectionsInOrder() throws IOException {
		String description = GetOnBoardParser.parse(fixture()).get(0).detail().description();

		assertThat(description).containsSubsequence("Desenvolver APIs.", "Perfil Java.", "Spring Boot.", "• Seguro");
	}

	@Test
	void mapsHybridAndFillsAnEmptyDescription() throws IOException {
		SourceJob hybrid = GetOnBoardParser.parse(fixture()).get(1);

		assertThat(hybrid.detail().workModel()).isEqualTo(WorkModel.HYBRID);
		assertThat(hybrid.detail().description()).isEqualTo("Sem descrição.");
		assertThat(hybrid.location()).isEqualTo("Remoto");
	}

	@Test
	void ignoresBrokenPayloads() {
		assertThat(GetOnBoardParser.parse("não é json")).isEmpty();
		assertThat(GetOnBoardParser.parse("{\"data\":null}")).isEmpty();
	}
}
