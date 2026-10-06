package com.diegohaefliger.atsresumeoptimizer.ai.application;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface PromptTemplateMapper {

	PromptTemplateView toView(PromptTemplate template);

	PromptTemplateSummary toSummary(PromptTemplate template);

	List<PromptTemplateSummary> toSummaries(List<PromptTemplate> templates);
}
