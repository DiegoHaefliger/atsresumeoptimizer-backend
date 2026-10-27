package com.diegohaefliger.atsresumeoptimizer.linkedin.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysis;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysisSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface LinkedInAnalysisWebMapper {

	@Mapping(target = "targetFit", source = "report.targetFit")
	@Mapping(target = "topActions", source = "report.topActions")
	@Mapping(target = "recruiterKeywords", source = "report.recruiterKeywords")
	@Mapping(target = "missingKeywords", source = "report.missingKeywords")
	@Mapping(target = "sections", source = "report.sections")
	LinkedInAnalysisResponse toResponse(LinkedInAnalysis analysis);

	LinkedInAnalysisSummaryResponse toSummaryResponse(LinkedInAnalysisSummary summary);
}
