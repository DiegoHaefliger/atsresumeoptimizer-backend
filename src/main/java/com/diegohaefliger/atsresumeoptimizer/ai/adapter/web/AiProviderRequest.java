package com.diegohaefliger.atsresumeoptimizer.ai.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.HttpUrl;
import com.diegohaefliger.atsresumeoptimizer.ai.application.AiProvider;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record AiProviderRequest(
		@NotNull AiProvider provider,
		boolean enabled,
		@Size(max = 500) String apiKey,
		@Size(max = 500) @HttpUrl String baseUrl,
		@Size(max = 100) String model,
		@Size(max = 100) String heavyModel) {
}
