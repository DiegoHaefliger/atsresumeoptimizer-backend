package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;

public interface AnalysisFeedbackService {

	void submit(AnalysisId analysisId, int rating, String comment);
}
