package com.diegohaefliger.atsresumeoptimizer.jobsearch.adapter.web;

import java.util.List;

record JobSuggestionPageResponse(
		List<JobSuggestionResponse> items,
		int page,
		int size,
		long totalItems,
		int totalPages,
		List<String> sources) {
}
