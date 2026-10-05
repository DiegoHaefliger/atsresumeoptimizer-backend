package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;

public interface AnalysisQueryService {

	AnalysisReportView get(AnalysisId id);

	AnalysisAtsView getAtsView(AnalysisId id);
}
