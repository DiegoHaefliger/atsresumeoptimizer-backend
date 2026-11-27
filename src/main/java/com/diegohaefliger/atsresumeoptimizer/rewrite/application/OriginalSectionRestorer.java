package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Formação e idiomas não são reescritos pra vaga: o texto lido do original vale mais que a versão da IA. */
final class OriginalSectionRestorer {

	private OriginalSectionRestorer() {
	}

	static StructuredResume apply(StructuredResume content, List<Section> originalSections) {
		List<ResumeSection> imported = originalSections.stream().map(ParsedResumeImporter::section).toList();
		List<ResumeSection> sections = new ArrayList<>(content.sections());
		original(imported, ResumeSectionSemanticType.EDUCATION)
				.filter(education -> education.kind() == ResumeSectionKind.ENTRIES)
				.ifPresent(education -> replaceUnless(sections, education, ResumeSectionKind.ENTRIES));
		original(imported, ResumeSectionSemanticType.LANGUAGES)
				.filter(languages -> !languages.keyValues().isEmpty())
				.ifPresent(languages -> replaceUnless(sections, languages, ResumeSectionKind.KEY_VALUE));
		return content.withSections(sections);
	}

	private static Optional<ResumeSection> original(List<ResumeSection> imported, ResumeSectionSemanticType type) {
		return imported.stream().filter(section -> section.semanticType() == type).findFirst();
	}

	private static void replaceUnless(List<ResumeSection> sections, ResumeSection original, ResumeSectionKind usableKind) {
		for (int index = 0; index < sections.size(); index++) {
			ResumeSection current = sections.get(index);
			if (current.semanticType() == original.semanticType()) {
				if (current.kind() != usableKind || isEmpty(current)) {
					sections.set(index, original);
				}
				return;
			}
		}
		sections.add(original);
	}

	private static boolean isEmpty(ResumeSection section) {
		return section.entries().isEmpty() && section.keyValues().isEmpty();
	}
}
