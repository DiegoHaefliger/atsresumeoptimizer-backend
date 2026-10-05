package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import java.util.Comparator;
import java.util.Map;
import org.springframework.util.StringUtils;

/** Título fixo por tipo: variação como "PROJETOS PESSOAIS" sobrevivia à reescrita e virava achado no D2. */
final class CanonicalSections {

	private static final Map<ResumeSectionSemanticType, String> CANONICAL_TITLE = Map.of(
			ResumeSectionSemanticType.SUMMARY, "Resumo Profissional",
			ResumeSectionSemanticType.EXPERIENCE, "Experiência Profissional",
			ResumeSectionSemanticType.PROJECTS, "Projetos",
			ResumeSectionSemanticType.EDUCATION, "Formação Acadêmica",
			ResumeSectionSemanticType.SKILLS, "Competências Técnicas",
			ResumeSectionSemanticType.CERTIFICATIONS, "Cursos e Certificações",
			ResumeSectionSemanticType.LANGUAGES, "Idiomas");

	private CanonicalSections() {
	}

	static StructuredResume apply(StructuredResume content) {
		return content.withSections(content.sections().stream()
				.filter(CanonicalSections::hasContent)
				.map(section -> CANONICAL_TITLE.containsKey(section.semanticType())
						? section.withTitle(CANONICAL_TITLE.get(section.semanticType()))
						: section)
				.sorted(Comparator.comparing(ResumeSection::semanticType))
				.toList());
	}

	private static boolean hasContent(ResumeSection section) {
		return switch (section.kind()) {
			case PARAGRAPH -> StringUtils.hasText(section.paragraph());
			case KEY_VALUE -> !section.keyValues().isEmpty();
			case RICH_LINES -> !section.richLines().isEmpty();
			case ENTRIES -> !section.entries().isEmpty();
		};
	}
}
