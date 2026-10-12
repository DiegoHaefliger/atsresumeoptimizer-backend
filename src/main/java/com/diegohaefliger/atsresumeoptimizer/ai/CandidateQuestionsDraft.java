package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;

public record CandidateQuestionsDraft(List<CandidateQuestionDraft> questions) {

	public CandidateQuestionsDraft {
		questions = questions == null ? List.of() : List.copyOf(questions);
	}
}
