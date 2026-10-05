package com.diegohaefliger.atsresumeoptimizer.analysis;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisStatus;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import java.util.UUID;

public record AnalysisSnapshot(
		UUID resumeVersionId,
		AnalysisStatus status,
		Integer overallScore,
		AnalysisMode mode,
		String jobDescription,
		String targetRole) {
}
