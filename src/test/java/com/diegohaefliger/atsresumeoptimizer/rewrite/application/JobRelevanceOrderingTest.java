package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import java.util.List;
import org.junit.jupiter.api.Test;

class JobRelevanceOrderingTest {

	@Test
	void putsTheBulletsAndTechnologiesThatMatchTheJobFirstKeepingTheRestInOrder() {
		ResumeEntry entry = new ResumeEntry("Dev", null, null, null, List.of(
				List.of(new TextSpan("Suporte ao time comercial.", false)),
				List.of(new TextSpan("Treinamentos internos.", false)),
				List.of(new TextSpan("Microsserviços em Java com Spring Boot.", false)),
				List.of(new TextSpan("APIs REST em Java.", false))), "Delphi, IoT, Java, Spring Boot");
		StructuredResume content = new StructuredResume("Ana", null, List.of(new ResumeSection("EXPERIÊNCIA",
				ResumeSectionSemanticType.EXPERIENCE, ResumeSectionKind.ENTRIES, null, null, null, List.of(entry))));

		ResumeEntry result = new JobRelevanceOrdering(List.of("Java", "Spring Boot")).apply(content)
				.sections().getFirst().entries().getFirst();

		assertThat(result.bullets()).extracting(bullet -> bullet.getFirst().text()).containsExactly(
				"Microsserviços em Java com Spring Boot.", "APIs REST em Java.", "Suporte ao time comercial.",
				"Treinamentos internos.");
		assertThat(result.technologies()).isEqualTo("Java, Spring Boot, Delphi, IoT");
	}
}
