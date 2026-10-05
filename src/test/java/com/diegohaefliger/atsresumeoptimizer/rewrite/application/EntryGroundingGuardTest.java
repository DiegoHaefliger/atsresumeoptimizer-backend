package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.ai.RequirementEvidence;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.TechVocabulary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EntryGroundingGuardTest {

	private static final List<String> KNOWN_TERMS = List.of("kafka", "dynatrace", "java", "mensageria");

	private static final TechVocabulary VOCABULARY = text -> {
		String normalized = NormalizedText.of(text);
		Set<String> found = new HashSet<>();
		KNOWN_TERMS.stream().filter(term -> NormalizedText.containsWord(normalized, term)).forEach(found::add);
		return found;
	};

	private static final String EXPERIENCE_TEXT = """
			EXPERIÊNCIA PROFISSIONAL
			Analista Sênior — 2024 – 2026
			Porto | São Paulo–SP
			• Monitoramento de incidentes com Dynatrace.
			Tecnologias: Java, Dynatrace
			PROJETOS
			Argos
			Plataforma de vagas com Kafka.
			Tecnologias: Java, Kafka""";

	private static final List<String> ORIGINAL_BULLETS = List.of("Monitoramento de incidentes com Dynatrace.");

	private final EntryGroundingGuard guard = new EntryGroundingGuard(VOCABULARY, EXPERIENCE_TEXT, ORIGINAL_BULLETS, List.of());

	@Test
	void keepsATermThatTheSameJobAlreadyCites() {
		ResumeEntry porto = porto("Dynatrace no monitoramento de incidentes.", "Dynatrace, Java");

		ResumeEntry result = firstEntry(guard.apply(resume(porto)));

		assertThat(result.bullets().getFirst().getFirst().text()).isEqualTo("Dynatrace no monitoramento de incidentes.");
		assertThat(result.technologies()).isEqualTo("Dynatrace, Java");
	}

	@Test
	void bringsBackTheOriginalBulletWhenTheAiMovedATermFromAnotherProject() {
		ResumeEntry porto = porto("Kafka e Dynatrace no monitoramento de incidentes.", "Kafka, Java, Dynatrace");

		ResumeEntry result = firstEntry(guard.apply(resume(porto)));

		assertThat(result.bullets().getFirst().getFirst().text()).isEqualTo("Monitoramento de incidentes com Dynatrace.");
		assertThat(result.technologies()).isEqualTo("Java, Dynatrace");
	}

	@Test
	void allowsTheJobTermOnlyWhereItsEvidenceIs() {
		var guardWithEvidence = new EntryGroundingGuard(VOCABULARY, EXPERIENCE_TEXT, ORIGINAL_BULLETS,
				List.of(new RequirementEvidence("mensageria", "Kafka")));
		ResumeEntry porto = porto("Mensageria e Dynatrace no monitoramento de incidentes.", "Java, Dynatrace");
		ResumeEntry argos = new ResumeEntry("Argos", null, null, "Plataforma de vagas com mensageria (Kafka).", List.of(),
				"Java, Kafka");
		StructuredResume content = new StructuredResume("Ana", null, List.of(
				new ResumeSection("EXPERIÊNCIA", ResumeSectionSemanticType.EXPERIENCE, ResumeSectionKind.ENTRIES, null, null, null,
						List.of(porto)),
				new ResumeSection("PROJETOS", ResumeSectionSemanticType.PROJECTS, ResumeSectionKind.ENTRIES, null, null, null,
						List.of(argos))));

		StructuredResume result = guardWithEvidence.apply(content);

		assertThat(firstEntry(result).bullets().getFirst().getFirst().text())
				.isEqualTo("Monitoramento de incidentes com Dynatrace.");
		assertThat(result.sections().getLast().entries().getFirst().context())
				.isEqualTo("Plataforma de vagas com mensageria (Kafka).");
	}

	private static ResumeEntry porto(String bullet, String technologies) {
		return new ResumeEntry("Analista Sênior", "2024 – 2026", "Porto | São Paulo–SP", null,
				List.of(List.of(new TextSpan(bullet, false))), technologies);
	}

	private static StructuredResume resume(ResumeEntry porto) {
		ResumeEntry argos = new ResumeEntry("Argos", null, null, "Plataforma de vagas com Kafka.", List.of(), "Java, Kafka");
		return new StructuredResume("Ana", null, List.of(
				new ResumeSection("EXPERIÊNCIA", ResumeSectionSemanticType.EXPERIENCE, ResumeSectionKind.ENTRIES, null, null, null,
						List.of(porto)),
				new ResumeSection("PROJETOS", ResumeSectionSemanticType.PROJECTS, ResumeSectionKind.ENTRIES, null, null, null,
						List.of(argos))));
	}

	private static ResumeEntry firstEntry(StructuredResume content) {
		return content.sections().getFirst().entries().getFirst();
	}
}
