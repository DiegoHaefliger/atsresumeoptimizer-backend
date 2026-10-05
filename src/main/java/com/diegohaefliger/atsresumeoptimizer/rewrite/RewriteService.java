package com.diegohaefliger.atsresumeoptimizer.rewrite;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.JobHighlight;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;

public interface RewriteService {

	RewriteResult rewrite(AnalysisId analysisId, ResumeTemplate template, JobHighlight jobHighlight);
}
