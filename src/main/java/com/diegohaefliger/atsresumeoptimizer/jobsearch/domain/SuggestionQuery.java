package com.diegohaefliger.atsresumeoptimizer.jobsearch.domain;

public record SuggestionQuery(
		String text,
		String source,
		Integer minScore,
		Integer postedWithinDays,
		SuggestionSort sort,
		int page,
		int size) {
}
