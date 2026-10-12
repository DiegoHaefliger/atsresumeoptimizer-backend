package com.diegohaefliger.atsresumeoptimizer.coverletter.adapter.web;

import java.time.Instant;
import java.util.UUID;

record CoverLetterResponse(UUID id, UUID jobPostingId, UUID resumeVersionId, String content, String aiModel,
		Instant createdAt, Instant updatedAt) {
}
