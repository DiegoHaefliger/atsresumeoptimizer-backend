package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewritePhase;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewriteProgressService;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
class RewriteProgressTracker implements RewriteProgressService {

	private final Map<UUID, RewritePhase> phaseByAnalysis = new ConcurrentHashMap<>();

	void advance(UUID analysisId, RewritePhase phase) {
		phaseByAnalysis.put(analysisId, phase);
	}

	void finish(UUID analysisId) {
		phaseByAnalysis.remove(analysisId);
	}

	@Override
	public Optional<RewritePhase> currentPhase(AnalysisId analysisId) {
		return Optional.ofNullable(phaseByAnalysis.get(analysisId.value()));
	}
}
