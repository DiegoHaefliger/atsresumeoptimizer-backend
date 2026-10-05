package com.diegohaefliger.atsresumeoptimizer.ai.application;

import java.time.Duration;

record AiRuntimeSettings(
		AiProvider provider,
		String apiKey,
		String baseUrl,
		String model,
		String heavyModel,
		double temperature,
		int maxOutputTokens,
		Duration timeout) {

	String modelFor(boolean heavyTask) {
		return heavyTask && heavyModel != null && !heavyModel.isBlank() ? heavyModel : model;
	}
}
