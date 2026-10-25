package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Linha de competências técnicas que só repete o que já está em Habilidades sai: comportamental não é competência técnica. */
final class SoftSkillDeduplicator {

	private static final String ITEM_SEPARATOR = "[,;]";

	private SoftSkillDeduplicator() {
	}

	static StructuredResume apply(StructuredResume content) {
		Set<String> soft = content.sections().stream()
				.filter(section -> section.semanticType() == ResumeSectionSemanticType.SOFT_SKILLS)
				.flatMap(section -> section.keyValues().stream())
				.flatMap(line -> items(line.value()).stream())
				.collect(Collectors.toSet());
		if (soft.isEmpty()) {
			return content;
		}
		return content.withSections(content.sections().stream()
				.map(section -> section.semanticType() == ResumeSectionSemanticType.SKILLS ? withoutRepeats(section, soft) : section)
				.toList());
	}

	private static ResumeSection withoutRepeats(ResumeSection section, Set<String> soft) {
		return section.withKeyValues(section.keyValues().stream().filter(line -> !repeats(line, soft)).toList());
	}

	private static boolean repeats(KeyValueLine line, Set<String> soft) {
		List<String> items = items(line.value());
		return !items.isEmpty() && soft.containsAll(items);
	}

	private static List<String> items(String value) {
		return Arrays.stream(NormalizedText.of(value).split(ITEM_SEPARATOR)).map(String::strip).filter(item -> !item.isEmpty()).toList();
	}
}
