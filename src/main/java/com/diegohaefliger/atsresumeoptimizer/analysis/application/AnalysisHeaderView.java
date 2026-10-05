package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisStatus;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import java.util.UUID;

public record AnalysisHeaderView(UUID id, AnalysisStatus status, AnalysisMode mode, boolean hasJobContext) {
}
