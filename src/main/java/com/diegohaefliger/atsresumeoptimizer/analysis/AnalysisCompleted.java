package com.diegohaefliger.atsresumeoptimizer.analysis;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisStatus;
import java.util.UUID;

public record AnalysisCompleted(UUID analysisId, AnalysisStatus status) {
}
