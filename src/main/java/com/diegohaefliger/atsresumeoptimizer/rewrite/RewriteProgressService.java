package com.diegohaefliger.atsresumeoptimizer.rewrite;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import java.util.Optional;

public interface RewriteProgressService {

	Optional<RewritePhase> currentPhase(AnalysisId analysisId);
}
