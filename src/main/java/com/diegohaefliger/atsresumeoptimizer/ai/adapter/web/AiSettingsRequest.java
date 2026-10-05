package com.diegohaefliger.atsresumeoptimizer.ai.adapter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

record AiSettingsRequest(
		@NotNull @DecimalMin("0.0") @DecimalMax("2.0") BigDecimal temperature,
		@Min(256) @Max(128_000) int maxOutputTokens,
		@Min(10) @Max(600) int timeoutSeconds,
		@NotEmpty List<@Valid AiProviderRequest> providers) {
}
