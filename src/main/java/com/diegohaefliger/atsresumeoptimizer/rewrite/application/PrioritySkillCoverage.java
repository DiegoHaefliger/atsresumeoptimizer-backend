package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.ai.PrioritySkill;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Competência prioritária com experiência comprovada tem que aparecer num bullet de cargo ou projeto. */
final class PrioritySkillCoverage {

	private PrioritySkillCoverage() {
	}

	static List<PrioritySkill> missing(StructuredResume content, List<PrioritySkill> prioritySkills) {
		String bulletText = NormalizedText.of(content.sections().stream()
				.filter(section -> section.semanticType().holdsBullets())
				.flatMap(section -> section.entries().stream())
				.flatMap(PrioritySkillCoverage::highlightableText)
				.collect(Collectors.joining("\n")));
		return prioritySkills.stream()
				.filter(PrioritySkill::usedInExperience)
				.filter(skill -> !NormalizedText.containsWord(bulletText, NormalizedText.of(skill.term()))
						&& !NormalizedText.containsWord(bulletText, NormalizedText.of(skill.evidenceOrTerm())))
				.toList();
	}

	private static Stream<String> highlightableText(ResumeEntry entry) {
		return Stream.concat(entry.bullets().stream().map(TextSpan::plainText),
				Stream.of(entry.context() == null ? "" : entry.context()));
	}
}
