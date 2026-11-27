package com.diegohaefliger.atsresumeoptimizer.jobsearch.domain;

import java.util.List;

public record SuggestionPage(
		List<JobSuggestion> items,
		int page,
		int size,
		long totalItems,
		int totalPages,
		List<String> sources) {
}
