package com.diegohaefliger.atsresumeoptimizer.interview.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InterviewQuestionSet(
		UUID id,
		UUID jobPostingId,
		UUID resumeVersionId,
		String aiModel,
		Instant createdAt,
		List<InterviewQuestion> questions) {

	public InterviewQuestionSet {
		questions = List.copyOf(questions);
	}
}
