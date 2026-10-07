package com.diegohaefliger.atsresumeoptimizer.coverletter.domain;

import java.time.Instant;
import java.util.UUID;

public record CoverLetter(
		UUID id,
		UUID jobPostingId,
		UUID analysisId,
		String content,
		String aiModel,
		Instant createdAt,
		Instant updatedAt) {
}
