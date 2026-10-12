package com.diegohaefliger.atsresumeoptimizer.candidatequestion.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CandidateQuestionSet(
		UUID id, UUID jobPostingId, String aiModel, Instant createdAt, List<CandidateQuestion> questions) {

	public CandidateQuestionSet {
		questions = List.copyOf(questions);
	}
}
