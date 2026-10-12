package com.diegohaefliger.atsresumeoptimizer.candidatequestion.adapter.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record CandidateQuestionSetResponse(UUID id, UUID jobPostingId, String aiModel, Instant createdAt,
		List<CandidateQuestionResponse> questions) {
}
