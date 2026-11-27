package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import java.util.List;
import org.junit.jupiter.api.Test;

class OriginalSectionRestorerTest {

	private static final List<Section> ORIGINAL = List.of(
			new Section("Formação Acadêmica", true, "Tecnólogo em Logística — UNINTER | 2016 – 2018\n"),
			new Section("Idiomas", true, "Inglês - Básico\n"));

	private static final ResumeSection AI_EDUCATION_AS_TEXT = new ResumeSection("Formação", ResumeSectionSemanticType.EDUCATION,
			ResumeSectionKind.RICH_LINES, null, null,
			List.of(List.of(new TextSpan("Tecnólogo em Logística", true), new TextSpan(" UNINTER 2016 2018", false))), null);

	@Test
	void usesTheOriginalEducationEntriesWhenTheAiFlattenedThemIntoText() {
		StructuredResume restored = OriginalSectionRestorer.apply(new StructuredResume("Ana", null, List.of(AI_EDUCATION_AS_TEXT)),
				ORIGINAL);

		ResumeSection education = restored.sections().getFirst();
		assertThat(education.kind()).isEqualTo(ResumeSectionKind.ENTRIES);
		assertThat(education.entries().getFirst().heading()).isEqualTo("Tecnólogo em Logística");
		assertThat(education.entries().getFirst().subheading()).isEqualTo("UNINTER");
	}

	@Test
	void bringsBackTheLanguagesTheAiLeftOut() {
		StructuredResume restored = OriginalSectionRestorer.apply(new StructuredResume("Ana", null, List.of(AI_EDUCATION_AS_TEXT)),
				ORIGINAL);

		assertThat(restored.sections()).extracting(ResumeSection::semanticType)
				.containsExactly(ResumeSectionSemanticType.EDUCATION, ResumeSectionSemanticType.LANGUAGES);
		assertThat(restored.sections().get(1).keyValues()).containsExactly(new KeyValueLine("Inglês", "Básico"));
	}

	@Test
	void keepsTheAiLanguagesWhenItReturnedThem() {
		ResumeSection aiLanguages = new ResumeSection("Idiomas", ResumeSectionSemanticType.LANGUAGES, ResumeSectionKind.KEY_VALUE,
				null, List.of(new KeyValueLine("Inglês", "Básico (leitura)")), null, null);

		StructuredResume restored = OriginalSectionRestorer.apply(new StructuredResume("Ana", null, List.of(aiLanguages)),
				List.of(new Section("Idiomas", true, "Inglês - Básico\n")));

		assertThat(restored.sections()).containsExactly(aiLanguages);
	}
}
