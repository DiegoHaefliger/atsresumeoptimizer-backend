package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysis;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysisSummary;
import org.mapstruct.Mapper;

@Mapper
interface LinkedInAnalysisEntityMapper {

	LinkedInAnalysis toDomain(LinkedInAnalysisEntity entity);

	LinkedInAnalysisSummary toSummary(LinkedInAnalysisHead head);
}
