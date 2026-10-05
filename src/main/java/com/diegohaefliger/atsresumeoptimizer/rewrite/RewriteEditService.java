package com.diegohaefliger.atsresumeoptimizer.rewrite;

import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;

public interface RewriteEditService {

	ComposedResume saveEdited(
			AnalysisId analysisId, ResumeTemplate template, StructuredResume content, ResumeContact contact);
}
