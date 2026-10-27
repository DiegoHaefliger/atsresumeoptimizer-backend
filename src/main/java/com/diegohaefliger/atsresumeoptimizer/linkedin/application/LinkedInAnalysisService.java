package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysis;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysisSummary;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkedInAnalysisService {

	LinkedInAnalysis analyze(UUID snapshotId);

	Optional<LinkedInAnalysis> latest();

	LinkedInAnalysis get(UUID id);

	List<LinkedInAnalysisSummary> history();
}
