package com.diegohaefliger.atsresumeoptimizer.coverletter.application;

import com.diegohaefliger.atsresumeoptimizer.ai.AiStage;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsageSource;
import com.diegohaefliger.atsresumeoptimizer.ai.ModelUsage;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class CoverLetterAiUsageSource implements AiUsageSource {

	private final CoverLetterRepository repository;

	CoverLetterAiUsageSource(CoverLetterRepository repository) {
		this.repository = repository;
	}

	@Override
	public AiStage stage() {
		return AiStage.RESUME_ADAPTATION;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ModelUsage> usageByModel(UUID jobPostingId) {
		return repository.usageByModelForJobPostingId(jobPostingId);
	}
}
