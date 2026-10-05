package com.diegohaefliger.atsresumeoptimizer.ai.application;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
		double temperature,
		int maxOutputTokens,
		Duration timeout,
		Map<String, TaskOverride> tasks,
		String settingsSecret,
		String settingsSecretFile) {

	public AiProperties {
		tasks = tasks == null ? Map.of() : Map.copyOf(tasks);
	}

	public int maxOutputTokensFor(String task, int fallback) {
		TaskOverride override = tasks.get(task);
		return override != null && override.maxOutputTokens() != null ? override.maxOutputTokens() : fallback;
	}

	public record TaskOverride(Integer maxOutputTokens) {
	}
}
