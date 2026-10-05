package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisStatus;

public record AnalysisCreation(AnalysisId id, AnalysisStatus status) {
}
