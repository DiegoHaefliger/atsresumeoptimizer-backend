package com.diegohaefliger.atsresumeoptimizer.analysis.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisStatus;
import java.util.UUID;

public record AnalysisCreatedResponse(UUID id, AnalysisStatus status) {
}
