package com.diegohaefliger.atsresumeoptimizer.ai.application;

import java.util.List;

public interface PromptTemplateService {

	List<PromptTemplateSummary> listLatest();

	PromptTemplateView current(String key);

	List<PromptTemplateSummary> history(String key);

	PromptTemplateView version(String key, int version);

	PromptTemplateView publish(String key, PromptTemplateUpdate update);
}
