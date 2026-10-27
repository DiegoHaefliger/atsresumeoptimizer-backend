package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;

public record LinkedInSectionReviewDraft(
		String section,
		Integer score,
		String diagnosis,
		List<LinkedInSuggestionDraft> suggestions,
		String rewrite,
		List<LinkedInItemReviewDraft> items) {

	public LinkedInSectionReviewDraft {
		suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
		items = items == null ? List.of() : List.copyOf(items);
	}
}
