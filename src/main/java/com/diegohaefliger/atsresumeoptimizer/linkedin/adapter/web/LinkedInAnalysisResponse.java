package com.diegohaefliger.atsresumeoptimizer.linkedin.adapter.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record LinkedInAnalysisResponse(
		UUID id,
		UUID snapshotId,
		Instant createdAt,
		int overallScore,
		String summary,
		String targetFit,
		List<String> topActions,
		List<String> recruiterKeywords,
		List<String> missingKeywords,
		List<LinkedInSectionReviewResponse> sections,
		boolean imagesAnalyzed,
		String aiModel,
		BigDecimal costUsd) {
}
