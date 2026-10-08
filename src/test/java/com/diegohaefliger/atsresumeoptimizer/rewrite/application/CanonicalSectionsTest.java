package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import java.util.List;
import org.junit.jupiter.api.Test;

class CanonicalSectionsTest {

	@Test
	void keepsObjectiveTitleInsteadOfRenamingItToSummary() {
		StructuredResume content = new StructuredResume("Ana", "Dev", List.of(
				paragraph("OBJETIVO", "Atuar como desenvolvedora."),
				paragraph("Perfil", "15 anos de experiência.")));

		List<String> titles = CanonicalSections.apply(content).sections().stream().map(ResumeSection::title).toList();

		assertThat(titles).containsExactly("OBJETIVO", "Resumo Profissional");
	}

	@Test
	void putsTheObjectiveFirstEvenWhenTheAiTypedItAsOther() {
		ResumeSection objective = new ResumeSection("Objetivo", ResumeSectionSemanticType.OTHER, ResumeSectionKind.PARAGRAPH,
				"Atuar como desenvolvedora.", null, null, null);
		StructuredResume content = new StructuredResume("Ana", "Dev", List.of(paragraph("Resumo", "15 anos."), objective));

		List<String> titles = CanonicalSections.apply(content).sections().stream().map(ResumeSection::title).toList();

		assertThat(titles).containsExactly("Objetivo", "Resumo Profissional");
	}

	private static ResumeSection paragraph(String title, String text) {
		return new ResumeSection(title, ResumeSectionSemanticType.SUMMARY, ResumeSectionKind.PARAGRAPH, text, null, null,
				null);
	}
}
