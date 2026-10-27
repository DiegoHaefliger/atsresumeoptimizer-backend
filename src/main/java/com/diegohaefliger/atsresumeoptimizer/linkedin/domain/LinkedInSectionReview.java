package com.diegohaefliger.atsresumeoptimizer.linkedin.domain;

import java.util.List;

public record LinkedInSectionReview(
		ProfileSection section, int score, String diagnosis, List<LinkedInSuggestion> suggestions, String rewrite) {

	public LinkedInSectionReview {
		suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
	}
}
