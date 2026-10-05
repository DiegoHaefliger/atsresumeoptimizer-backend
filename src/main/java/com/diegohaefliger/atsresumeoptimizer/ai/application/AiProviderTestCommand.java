package com.diegohaefliger.atsresumeoptimizer.ai.application;

import java.math.BigDecimal;

public record AiProviderTestCommand(
		AiProvider provider,
		String apiKey,
		String baseUrl,
		String model,
		String heavyModel,
		BigDecimal temperature,
		int maxOutputTokens,
		int timeoutSeconds) {
}
