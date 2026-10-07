package com.diegohaefliger.atsresumeoptimizer.coverletter.adapter.web;

import java.time.Instant;
import java.util.UUID;

record CoverLetterResponse(UUID id, UUID jobPostingId, String content, String aiModel, Instant updatedAt) {
}
