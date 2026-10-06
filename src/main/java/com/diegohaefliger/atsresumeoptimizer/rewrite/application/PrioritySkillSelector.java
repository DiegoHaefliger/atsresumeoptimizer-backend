package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.ai.PrioritySkill;
import com.diegohaefliger.atsresumeoptimizer.ai.RequirementEvidence;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.KeywordMatch;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** As principais da vaga (escolha do usuário ou as 5 primeiras) que o candidato tem, na ordem da vaga. */
final class PrioritySkillSelector {

	private PrioritySkillSelector() {
	}

	static List<PrioritySkill> select(List<KeywordMatch> matches, List<String> priorityKeywords,
			List<RequirementEvidence> evidences, List<Section> sections) {
		Set<String> priority = priorityKeywords.stream().map(NormalizedText::of).collect(Collectors.toSet());
		String experienceText = NormalizedText.of(sections.stream().filter(RewriteGuardRails::isExperienceSection)
				.map(Section::content).collect(Collectors.joining("\n")));
		Map<String, RequirementEvidence> evidenceByTerm = evidences.stream()
				.collect(Collectors.toMap(item -> NormalizedText.of(item.requirement()), Function.identity(), (a, b) -> a));
		return matches.stream()
				.filter(match -> priority.contains(NormalizedText.of(match.term())))
				.map(match -> skill(match, evidenceByTerm.get(NormalizedText.of(match.term())), experienceText))
				.flatMap(Optional::stream)
				.toList();
	}

	private static Optional<PrioritySkill> skill(KeywordMatch match, RequirementEvidence evidence,
			String experienceText) {
		if (match.found()) {
			boolean inExperience = match.sections().stream().anyMatch(PrioritySkillSelector::isExperienceTitle);
			return Optional.of(new PrioritySkill(match.term(), null, inExperience));
		}
		if (evidence == null) {
			return Optional.empty();
		}
		boolean inExperience = NormalizedText.containsWord(experienceText, NormalizedText.of(evidence.evidence()));
		return Optional.of(new PrioritySkill(match.term(), evidence.evidence(), inExperience));
	}

	private static boolean isExperienceTitle(String title) {
		return RewriteGuardRails.isExperienceSection(new Section(title, true, ""));
	}
}
