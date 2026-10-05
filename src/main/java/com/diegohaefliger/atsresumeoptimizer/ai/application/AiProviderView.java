package com.diegohaefliger.atsresumeoptimizer.ai.application;

import java.util.List;

public record AiProviderView(
		AiProvider provider,
		String label,
		boolean enabled,
		Integer priority,
		boolean requiresApiKey,
		boolean requiresBaseUrl,
		String defaultBaseUrl,
		List<String> suggestedModels,
		String model,
		String heavyModel,
		String baseUrl,
		ApiKeySource apiKeySource,
		String apiKeyHint) {
}
