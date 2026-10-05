package com.diegohaefliger.atsresumeoptimizer.resume;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ResumeSummary(
		UUID id,
		String title,
		Instant createdAt,
		ResumeOrigin origin,
		UUID sourceAnalysisId,
		boolean favorite,
		List<ResumeVersionSummary> versions) {

	public ResumeSummary {
		versions = List.copyOf(versions);
	}
}
