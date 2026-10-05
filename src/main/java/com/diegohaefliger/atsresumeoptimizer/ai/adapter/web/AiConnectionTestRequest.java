package com.diegohaefliger.atsresumeoptimizer.ai.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.HttpUrl;
import com.diegohaefliger.atsresumeoptimizer.ai.application.AiProvider;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

record AiConnectionTestRequest(
		@NotNull AiProvider provider,
		@Size(max = 500) String apiKey,
		@Size(max = 500) @HttpUrl String baseUrl,
		@NotBlank @Size(max = 100) String model,
		@Size(max = 100) String heavyModel,
		@NotNull @DecimalMin("0.0") @DecimalMax("2.0") BigDecimal temperature,
		@Min(256) @Max(128_000) int maxOutputTokens,
		@Min(10) @Max(600) int timeoutSeconds) {
}
