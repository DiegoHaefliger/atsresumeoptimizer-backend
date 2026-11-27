package com.diegohaefliger.atsresumeoptimizer.jobsearch.adapter.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionSort;

record SuggestionPageRequest(
		@PositiveOrZero Integer page,
		@Min(1) @Max(SuggestionPageRequest.MAX_SIZE) Integer size,
		String query,
		String source,
		@Min(0) @Max(100) Integer minScore,
		@Positive Integer postedWithinDays,
		SuggestionSort sort) {

	static final int DEFAULT_SIZE = 30;
	static final int MAX_SIZE = 100;

	SuggestionPageRequest {
		page = page == null ? 0 : page;
		size = size == null ? DEFAULT_SIZE : size;
		sort = sort == null ? SuggestionSort.SCORE_DESC : sort;
	}
}
