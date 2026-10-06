package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;

/** Só keywords que o currículo original já tem: keyword ausente nunca entra, pra IA não ter como incluí-la. */
public record JobFocus(
		String jobTitle, String seniority, List<String> matchedKeywords, List<RequirementEvidence> evidencedRequirements,
		List<PrioritySkill> prioritySkills) {

	public static final JobFocus NONE = new JobFocus(null, null, List.of(), List.of(), List.of());

	public JobFocus {
		matchedKeywords = matchedKeywords == null ? List.of() : List.copyOf(matchedKeywords);
		evidencedRequirements = evidencedRequirements == null ? List.of() : List.copyOf(evidencedRequirements);
		prioritySkills = prioritySkills == null ? List.of() : List.copyOf(prioritySkills);
	}

	public JobFocus(String jobTitle, String seniority, List<String> matchedKeywords,
			List<RequirementEvidence> evidencedRequirements) {
		this(jobTitle, seniority, matchedKeywords, evidencedRequirements, List.of());
	}

	public JobFocus(String jobTitle, String seniority, List<String> matchedKeywords) {
		this(jobTitle, seniority, matchedKeywords, List.of(), List.of());
	}

	public boolean enabled() {
		return jobTitle != null && !jobTitle.isBlank();
	}
}
