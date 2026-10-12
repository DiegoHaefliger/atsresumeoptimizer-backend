package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import java.util.List;
import org.junit.jupiter.api.Test;

class CanonicalSectionsTest {

	@Test
	void renamesSummaryVariantsToTheCanonicalTitle() {
		StructuredResume content = new StructuredResume("Ana", "Dev", List.of(paragraph("Perfil", "15 anos de experiência.")));

		List<String> titles = CanonicalSections.apply(content).sections().stream().map(ResumeSection::title).toList();

		assertThat(titles).containsExactly("Resumo Profissional");
	}

	@Test
	void placesHabilidadesRightAfterTechnicalSkills() {
		ResumeSection soft = new ResumeSection("HABILIDADES", ResumeSectionSemanticType.SOFT_SKILLS, ResumeSectionKind.KEY_VALUE,
				null, List.of(new KeyValueLine("Habilidades", "Comunicação")), null, null);
		ResumeSection skills = new ResumeSection("Skills", ResumeSectionSemanticType.SKILLS, ResumeSectionKind.KEY_VALUE,
				null, List.of(new KeyValueLine("Backend", "Java")), null, null);
		ResumeSection languages = new ResumeSection("Idiomas", ResumeSectionSemanticType.LANGUAGES, ResumeSectionKind.KEY_VALUE,
				null, List.of(new KeyValueLine("Inglês", "Avançado")), null, null);
		StructuredResume content = new StructuredResume("Ana", "Dev", List.of(languages, soft, skills));

		List<String> titles = CanonicalSections.apply(content).sections().stream().map(ResumeSection::title).toList();

		assertThat(titles).containsExactly("Competências Técnicas", "Habilidades", "Idiomas");
	}

	private static ResumeSection paragraph(String title, String text) {
		return new ResumeSection(title, ResumeSectionSemanticType.SUMMARY, ResumeSectionKind.PARAGRAPH, text, null, null,
				null);
	}
}
