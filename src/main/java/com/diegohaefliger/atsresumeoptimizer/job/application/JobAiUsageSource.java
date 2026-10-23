package com.diegohaefliger.atsresumeoptimizer.job.application;

import com.diegohaefliger.atsresumeoptimizer.ai.AiStage;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsageSource;
import com.diegohaefliger.atsresumeoptimizer.ai.ModelUsage;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class JobAiUsageSource implements AiUsageSource {

	private final JobPostingRepository repository;

	JobAiUsageSource(JobPostingRepository repository) {
		this.repository = repository;
	}

	@Override
	public AiStage stage() {
		return AiStage.JOB_ANALYSIS;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ModelUsage> usageByModel(UUID jobPostingId) {
		return repository.usageByModelForJobPostingId(jobPostingId);
	}
}
