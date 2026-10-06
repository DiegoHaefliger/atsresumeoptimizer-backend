package com.diegohaefliger.atsresumeoptimizer.ai.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.ai.application.PromptTemplateUpdate;
import org.mapstruct.Mapper;

@Mapper
interface PromptTemplateRequestMapper {

	PromptTemplateUpdate toUpdate(PromptTemplateRequest request);
}
