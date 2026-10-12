package com.diegohaefliger.atsresumeoptimizer.interview.adapter.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record InterviewQuestionSetResponse(UUID id, UUID jobPostingId, UUID resumeVersionId, String aiModel, Instant createdAt,
		List<InterviewQuestionResponse> questions) {
}
