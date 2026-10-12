package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import java.util.List;
import org.junit.jupiter.api.Test;

class ObjectiveMergerTest {

	private static final Section ORIGINAL_OBJECTIVE = new Section("Objetivo", true, "Atuar como desenvolvedora\nBack-end sênior.");

	@Test
	void appendsTheOriginalObjectiveToTheEndOfTheSummary() {
		StructuredResume content = new StructuredResume("Ana", "Dev", List.of(summary("Resumo Profissional", "15 anos.")));

		List<ResumeSection> sections = ObjectiveMerger.mergeIntoSummary(content, List.of(ORIGINAL_OBJECTIVE)).sections();

		assertThat(sections).extracting(ResumeSection::title).containsExactly("Resumo Profissional");
		assertThat(sections.getFirst().paragraph()).isEqualTo("15 anos. Atuar como desenvolvedora Back-end sênior.");
	}

	@Test
	void foldsTheAdaptedObjectiveSectionIntoTheSummaryAndDropsIt() {
		StructuredResume content = new StructuredResume("Ana", "Dev",
				List.of(summary("Resumo", "15 anos."), summary("Objetivo", "Atuar como Dev Java.")));

		List<ResumeSection> sections = ObjectiveMerger.mergeIntoSummary(content, List.of(ORIGINAL_OBJECTIVE)).sections();

		assertThat(sections).hasSize(1);
		assertThat(sections.getFirst().paragraph()).isEqualTo("15 anos. Atuar como Dev Java.");
	}

	@Test
	void createsTheSummaryWhenThereIsOnlyAnObjective() {
		StructuredResume content = new StructuredResume("Ana", "Dev", List.of());

		List<ResumeSection> sections = ObjectiveMerger.mergeIntoSummary(content, List.of(ORIGINAL_OBJECTIVE)).sections();

		assertThat(sections).extracting(ResumeSection::title).containsExactly("Resumo Profissional");
	}

	@Test
	void doesNotRepeatAnObjectiveAlreadyInsideTheSummary() {
		StructuredResume content = new StructuredResume("Ana", "Dev",
				List.of(summary("Resumo", "15 anos. Atuar como desenvolvedora Back-end sênior.")));

		assertThat(ObjectiveMerger.mergeIntoSummary(content, List.of(ORIGINAL_OBJECTIVE)).sections().getFirst().paragraph())
				.isEqualTo("15 anos. Atuar como desenvolvedora Back-end sênior.");
	}

	@Test
	void leavesContentUntouchedWhenThereIsNoObjective() {
		StructuredResume content = new StructuredResume("Ana", "Dev", List.of(summary("Resumo Profissional", "15 anos.")));

		assertThat(ObjectiveMerger.mergeIntoSummary(content, List.of(new Section("Perfil", true, "Texto")))).isSameAs(content);
	}

	private static ResumeSection summary(String title, String text) {
		return new ResumeSection(title, ResumeSectionSemanticType.SUMMARY, ResumeSectionKind.PARAGRAPH, text, null, null, null);
	}
}
