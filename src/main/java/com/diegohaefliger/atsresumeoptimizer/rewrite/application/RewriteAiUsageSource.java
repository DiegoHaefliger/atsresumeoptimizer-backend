package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.AiStage;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsageSource;
import com.diegohaefliger.atsresumeoptimizer.ai.ModelUsage;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisQueryService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class RewriteAiUsageSource implements AiUsageSource {

	private final RewriteRepository repository;
	private final AnalysisQueryService analysisQueryService;

	RewriteAiUsageSource(RewriteRepository repository, AnalysisQueryService analysisQueryService) {
		this.repository = repository;
		this.analysisQueryService = analysisQueryService;
	}

	@Override
	public AiStage stage() {
		return AiStage.RESUME_ADAPTATION;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ModelUsage> usageByModel(UUID jobPostingId) {
		List<UUID> analysisIds = analysisQueryService.analysisIds(jobPostingId);
		return analysisIds.isEmpty() ? List.of() : repository.usageByModelForAnalysisIds(analysisIds);
	}
}
