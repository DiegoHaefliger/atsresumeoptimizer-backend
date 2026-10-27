package com.diegohaefliger.atsresumeoptimizer.linkedin.domain;

import java.util.List;

public record LinkedInItemReview(
		String title, int score, String diagnosis, List<LinkedInSuggestion> suggestions, String rewrite) {

	public LinkedInItemReview {
		suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
	}
}
