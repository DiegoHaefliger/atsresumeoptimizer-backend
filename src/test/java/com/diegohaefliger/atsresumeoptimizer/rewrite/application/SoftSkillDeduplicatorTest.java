package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import java.util.List;
import org.junit.jupiter.api.Test;

class SoftSkillDeduplicatorTest {

	private static ResumeSection section(ResumeSectionSemanticType type, KeyValueLine... lines) {
		return new ResumeSection("Seção", type, ResumeSectionKind.KEY_VALUE, null, List.of(lines), null, null);
	}

	private static List<KeyValueLine> technicalLines(StructuredResume content) {
		return content.sections().stream().filter(section -> section.semanticType() == ResumeSectionSemanticType.SKILLS)
				.findFirst().orElseThrow().keyValues();
	}

	@Test
	void dropsTheTechnicalLineThatRepeatsTheHabilidadesSection() {
		StructuredResume content = new StructuredResume("Ana", "Dev", List.of(
				section(ResumeSectionSemanticType.SKILLS, new KeyValueLine("Linguagens", "Java, SQL"),
						new KeyValueLine("Comportamentais", "Comunicação, Liderança técnica, Trabalho em equipe")),
				section(ResumeSectionSemanticType.SOFT_SKILLS, new KeyValueLine("Habilidades",
						"Comunicação, Liderança técnica, Trabalho em equipe"))));

		StructuredResume result = SoftSkillDeduplicator.apply(content);

		assertThat(technicalLines(result)).extracting(KeyValueLine::label).containsExactly("Linguagens");
		assertThat(result.sections()).hasSize(2);
	}

	@Test
	void keepsATechnicalLineThatHasItemsOutsideHabilidades() {
		StructuredResume content = new StructuredResume("Ana", "Dev", List.of(
				section(ResumeSectionSemanticType.SKILLS, new KeyValueLine("Práticas", "Code review, Comunicação")),
				section(ResumeSectionSemanticType.SOFT_SKILLS, new KeyValueLine("Habilidades", "Comunicação"))));

		assertThat(technicalLines(SoftSkillDeduplicator.apply(content))).hasSize(1);
	}

	@Test
	void changesNothingWithoutAHabilidadesSection() {
		StructuredResume content = new StructuredResume("Ana", "Dev", List.of(
				section(ResumeSectionSemanticType.SKILLS, new KeyValueLine("Comportamentais", "Comunicação"))));

		assertThat(SoftSkillDeduplicator.apply(content)).isEqualTo(content);
	}
}
