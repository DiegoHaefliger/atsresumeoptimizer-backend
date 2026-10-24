package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class LinkedInHtmlParserTest {

	private static String fixture(String name) throws IOException {
		return new ClassPathResource("jobsearch/" + name).getContentAsString(StandardCharsets.UTF_8);
	}

	@Test
	void readsEachSearchCardAndSkipsTheOnesWithoutAnId() throws IOException {
		List<SourceJob> jobs = LinkedInHtmlParser.parseSearch(fixture("linkedin-search.html"));

		assertThat(jobs).hasSize(2);
		SourceJob first = jobs.getFirst();
		assertThat(first.externalId()).isEqualTo("1000000001");
		assertThat(first.title()).isEqualTo("Desenvolvedor Java Pleno");
		assertThat(first.company()).isEqualTo("Acme Tecnologia");
		assertThat(first.location()).isEqualTo("Curitiba, Paraná, Brazil");
		assertThat(first.postedOn()).isEqualTo(LocalDate.of(2026, 10, 23));
		assertThat(first.url()).isEqualTo("https://www.linkedin.com/jobs/view/desenvolvedor-java-pleno-at-acme-1000000001");
	}

	@Test
	void keepsTheCardWhenTheDateIsMissingOrInvalid() throws IOException {
		SourceJob second = LinkedInHtmlParser.parseSearch(fixture("linkedin-search.html")).get(1);

		assertThat(second.externalId()).isEqualTo("1000000002");
		assertThat(second.postedOn()).isNull();
	}

	@Test
	void returnsNothingForAnEmptyPage() {
		assertThat(LinkedInHtmlParser.parseSearch("")).isEmpty();
	}

	@Test
	void readsTheDescriptionWithLineBreaksAndBullets() throws IOException {
		SourceJobDetail detail = LinkedInHtmlParser.parseDetail(fixture("linkedin-detail.html")).orElseThrow();

		assertThat(detail.description()).startsWith("Sobre a vaga\n\nAtuar com Java e Spring Boot.\nModelo remoto.");
		assertThat(detail.description()).contains("• Vale-refeição").contains("• Plano de saúde");
		assertThat(detail.description()).doesNotContain("\n\n\n");
	}

	@Test
	void readsTheSeniorityAndEmploymentCriteria() throws IOException {
		SourceJobDetail detail = LinkedInHtmlParser.parseDetail(fixture("linkedin-detail.html")).orElseThrow();

		assertThat(detail.seniorityLevel()).isEqualTo("Mid-Senior level");
		assertThat(detail.employmentType()).isEqualTo("Full-time");
	}

	@Test
	void returnsNothingWhenThePageHasNoDescription() {
		assertThat(LinkedInHtmlParser.parseDetail("<html><body>login</body></html>")).isEmpty();
	}
}
