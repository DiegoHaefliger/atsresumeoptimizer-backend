package com.diegohaefliger.atsresumeoptimizer.jobsearch.adapter.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionStatus;

record JobSuggestionResponse(
		UUID id,
		String source,
		String externalId,
		String url,
		String title,
		String company,
		String location,
		LocalDate postedOn,
		String description,
		Integer score,
		SuggestionStatus status,
		Instant foundAt,
		Instant decidedAt,
		UUID jobPostingId) {
}
