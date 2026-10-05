package com.diegohaefliger.atsresumeoptimizer.ai.application;

public interface AiSettingsService {

	AiSettingsView current();

	AiSettingsView save(AiSettingsUpdate update);

	AiConnectionTestResult test(AiProviderTestCommand command);

	AiModelList listModels(AiModelQuery query);
}
