package com.diegohaefliger.atsresumeoptimizer.interview.application;

import com.diegohaefliger.atsresumeoptimizer.interview.domain.InterviewQuestionSet;
import java.util.List;
import java.util.UUID;

public interface InterviewQuestionService {

	/** Da geração mais nova para a mais antiga. */
	List<InterviewQuestionSet> list(UUID jobPostingId);

	/** Sem currículo, as dicas de resposta ficam só na linha de raciocínio esperada. */
	InterviewQuestionSet generate(UUID jobPostingId, UUID resumeVersionId);
}
