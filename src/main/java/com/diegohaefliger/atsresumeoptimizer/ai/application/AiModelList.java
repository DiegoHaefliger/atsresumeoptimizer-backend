package com.diegohaefliger.atsresumeoptimizer.ai.application;

import java.util.List;

public record AiModelList(AiProvider provider, AiModelListSource source, List<String> models, String message) {

	public AiModelList {
		models = List.copyOf(models);
	}
}
