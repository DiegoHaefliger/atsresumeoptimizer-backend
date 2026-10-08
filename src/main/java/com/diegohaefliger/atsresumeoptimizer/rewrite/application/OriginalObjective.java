package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** A IA funde "Objetivo" no resumo (ambos SUMMARY no prompt); o texto do candidato volta como seção própria. */
final class OriginalObjective {

	private static final String OBJECTIVE_TITLE_PREFIX = "objetivo";

	private OriginalObjective() {
	}

	static StructuredResume restore(StructuredResume content, List<Section> originalSections) {
		if (content.sections().stream().anyMatch(OriginalObjective::isObjective)) {
			return content;
		}
		return original(originalSections).map(objective -> withFirst(content, objective)).orElse(content);
	}

	static boolean isObjective(ResumeSection section) {
		return isObjectiveTitle(section.title());
	}

	private static boolean isObjectiveTitle(String title) {
		return NormalizedText.of(title).startsWith(OBJECTIVE_TITLE_PREFIX);
	}

	private static Optional<ResumeSection> original(List<Section> originalSections) {
		return originalSections.stream()
				.filter(section -> isObjectiveTitle(section.title()))
				.map(OriginalObjective::toSection)
				.filter(section -> !section.paragraph().isBlank())
				.findFirst();
	}

	private static ResumeSection toSection(Section section) {
		String paragraph = ResumeTextLine.of(section.content()).stream()
				.map(ResumeTextLine::text)
				.collect(Collectors.joining(" "));
		return new ResumeSection(section.title().strip(), ResumeSectionSemanticType.SUMMARY, ResumeSectionKind.PARAGRAPH,
				paragraph, null, null, null);
	}

	private static StructuredResume withFirst(StructuredResume content, ResumeSection objective) {
		List<ResumeSection> sections = new ArrayList<>();
		sections.add(objective);
		sections.addAll(content.sections());
		return content.withSections(sections);
	}
}
