package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysis;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysisSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface LinkedInAnalysisEntityMapper {

	@Mapping(target = "unchanged", constant = "false")
	LinkedInAnalysis toDomain(LinkedInAnalysisEntity entity);

	LinkedInAnalysisSummary toSummary(LinkedInAnalysisHead head);
}
