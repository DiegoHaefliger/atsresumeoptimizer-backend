package com.diegohaefliger.atsresumeoptimizer.rewrite;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import java.util.Optional;

public interface RewriteQueryService {

	/** Última adaptação salva da análise, com as edições; sem as comparações com o original, que não ficam gravadas. */
	Optional<RewriteResult> latest(AnalysisId analysisId);
}
