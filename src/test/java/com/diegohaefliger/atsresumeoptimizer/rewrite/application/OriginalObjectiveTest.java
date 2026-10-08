package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import java.util.List;
import org.junit.jupiter.api.Test;

class OriginalObjectiveTest {

	private static final Section ORIGINAL_OBJECTIVE = new Section("Objetivo", true, "Atuar como desenvolvedora\nBack-end sênior.");

	@Test
	void bringsOriginalObjectiveBackAheadOfTheSummary() {
		StructuredResume content = new StructuredResume("Ana", "Dev", List.of(summary("Resumo Profissional", "15 anos.")));

		List<ResumeSection> sections = OriginalObjective.restore(content, List.of(ORIGINAL_OBJECTIVE)).sections();

		assertThat(sections).extracting(ResumeSection::title).containsExactly("Objetivo", "Resumo Profissional");
		assertThat(sections.getFirst().paragraph()).isEqualTo("Atuar como desenvolvedora Back-end sênior.");
	}

	@Test
	void leavesContentUntouchedWhenObjectiveSurvivedTheRewrite() {
		StructuredResume content = new StructuredResume("Ana", "Dev", List.of(summary("Objetivo", "Já está aqui.")));

		assertThat(OriginalObjective.restore(content, List.of(ORIGINAL_OBJECTIVE))).isSameAs(content);
	}

	@Test
	void leavesContentUntouchedWhenOriginalHasNoObjective() {
		StructuredResume content = new StructuredResume("Ana", "Dev", List.of(summary("Resumo Profissional", "15 anos.")));

		assertThat(OriginalObjective.restore(content, List.of(new Section("Perfil", true, "Texto")))).isSameAs(content);
	}

	private static ResumeSection summary(String title, String text) {
		return new ResumeSection(title, ResumeSectionSemanticType.SUMMARY, ResumeSectionKind.PARAGRAPH, text, null, null, null);
	}
}
