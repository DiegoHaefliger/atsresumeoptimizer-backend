package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;

public record LinkedInItemReviewDraft(
		String title, Integer score, String diagnosis, List<LinkedInSuggestionDraft> suggestions, String rewrite) {

	public LinkedInItemReviewDraft {
		suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
	}
}
