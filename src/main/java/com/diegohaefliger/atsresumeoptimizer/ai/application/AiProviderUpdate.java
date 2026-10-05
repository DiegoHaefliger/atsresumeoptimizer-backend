package com.diegohaefliger.atsresumeoptimizer.ai.application;

public record AiProviderUpdate(
		AiProvider provider,
		boolean enabled,
		String apiKey,
		String baseUrl,
		String model,
		String heavyModel) {
}
