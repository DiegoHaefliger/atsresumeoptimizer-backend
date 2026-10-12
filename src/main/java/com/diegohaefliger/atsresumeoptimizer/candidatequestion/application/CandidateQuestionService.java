package com.diegohaefliger.atsresumeoptimizer.candidatequestion.application;

import com.diegohaefliger.atsresumeoptimizer.candidatequestion.domain.CandidateQuestionSet;
import java.util.List;
import java.util.UUID;

public interface CandidateQuestionService {

	/** Da geração mais nova para a mais antiga. */
	List<CandidateQuestionSet> list(UUID jobPostingId);

	CandidateQuestionSet generate(UUID jobPostingId);
}
