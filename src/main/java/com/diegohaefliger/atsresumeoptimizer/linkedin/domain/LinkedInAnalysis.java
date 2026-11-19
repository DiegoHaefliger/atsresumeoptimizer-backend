package com.diegohaefliger.atsresumeoptimizer.linkedin.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LinkedInAnalysis(
		UUID id,
		UUID snapshotId,
		Instant createdAt,
		int overallScore,
		String summary,
		LinkedInAnalysisReport report,
		boolean imagesAnalyzed,
		String aiModel,
		BigDecimal costUsd,
		boolean unchanged) {

	public LinkedInAnalysis reused() {
		return new LinkedInAnalysis(id, snapshotId, createdAt, overallScore, summary, report, imagesAnalyzed, aiModel, costUsd,
				true);
	}
}
