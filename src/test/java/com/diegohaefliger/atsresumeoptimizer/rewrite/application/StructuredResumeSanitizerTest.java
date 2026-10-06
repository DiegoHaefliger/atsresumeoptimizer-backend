package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import java.util.List;
import org.junit.jupiter.api.Test;

class StructuredResumeSanitizerTest {

	@Test
	void separatesCourseFromInstitutionWhenBoldSpanIsGluedToIt() {
		List<TextSpan> line = List.of(new TextSpan("Tecnólogo em Análise e Desenvolvimento de Sistemas", true),
				new TextSpan("UNINTER | 02/2016 - 12/2018", false));
		ResumeSection education = new ResumeSection("Formação Acadêmica", ResumeSectionSemanticType.EDUCATION,
				ResumeSectionKind.RICH_LINES, null, List.of(), List.of(line), List.of());

		StructuredResume sanitized = StructuredResumeSanitizer.sanitizeGrammar(
				new StructuredResume("Nome", null, List.of(education), List.of()));

		assertThat(TextSpan.plainText(sanitized.sections().getFirst().richLines().getFirst()))
				.isEqualTo("Tecnólogo em Análise e Desenvolvimento de Sistemas — UNINTER | 02/2016 - 12/2018");
	}

	@Test
	void separatesGluedCertificationTitleAndInstitution() {
		List<TextSpan> line = List.of(new TextSpan("Formação Engenheiro de Inteligência Artificial 4.0Data", true),
				new TextSpan(" Science Academy | 2024", false));
		ResumeSection certifications = new ResumeSection("Cursos e Certificações",
				ResumeSectionSemanticType.CERTIFICATIONS, ResumeSectionKind.RICH_LINES, null, List.of(), List.of(line),
				List.of());

		StructuredResume sanitized = StructuredResumeSanitizer.sanitizeGrammar(
				new StructuredResume("Nome", null, List.of(certifications), List.of()));

		assertThat(TextSpan.plainText(sanitized.sections().getFirst().richLines().getFirst()))
				.isEqualTo("Formação Engenheiro de Inteligência Artificial 4.0 — Data Science Academy | 2024");
	}
}
