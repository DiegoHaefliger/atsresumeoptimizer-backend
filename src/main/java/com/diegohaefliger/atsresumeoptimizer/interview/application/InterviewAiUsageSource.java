package com.diegohaefliger.atsresumeoptimizer.interview.application;

import com.diegohaefliger.atsresumeoptimizer.ai.AiStage;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsageSource;
import com.diegohaefliger.atsresumeoptimizer.ai.ModelUsage;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class InterviewAiUsageSource implements AiUsageSource {

	private final InterviewQuestionSetRepository repository;

	InterviewAiUsageSource(InterviewQuestionSetRepository repository) {
		this.repository = repository;
	}

	@Override
	public AiStage stage() {
		return AiStage.INTERVIEW_PREPARATION;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ModelUsage> usageByModel(UUID jobPostingId) {
		return repository.usageByModelForJobPostingId(jobPostingId);
	}
}
