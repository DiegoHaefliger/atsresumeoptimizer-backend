package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;

public record InterviewQuestionsDraft(List<InterviewQuestionDraft> questions) {

	public InterviewQuestionsDraft {
		questions = questions == null ? List.of() : List.copyOf(questions);
	}
}
