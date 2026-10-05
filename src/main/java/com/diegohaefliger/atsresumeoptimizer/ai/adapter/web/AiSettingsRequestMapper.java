package com.diegohaefliger.atsresumeoptimizer.ai.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.ai.application.AiModelQuery;
import com.diegohaefliger.atsresumeoptimizer.ai.application.AiProviderTestCommand;
import com.diegohaefliger.atsresumeoptimizer.ai.application.AiSettingsUpdate;
import org.mapstruct.Mapper;

@Mapper
interface AiSettingsRequestMapper {

	AiSettingsUpdate toUpdate(AiSettingsRequest request);

	AiProviderTestCommand toTestCommand(AiConnectionTestRequest request);

	AiModelQuery toQuery(AiModelListRequest request);
}
