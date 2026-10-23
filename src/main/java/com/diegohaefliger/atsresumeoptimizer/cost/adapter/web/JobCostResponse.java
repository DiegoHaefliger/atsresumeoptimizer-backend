package com.diegohaefliger.atsresumeoptimizer.cost.adapter.web;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

record JobCostResponse(@NotNull BigDecimal totalCostUsd, @NotNull BigDecimal totalCostBrl,
		@NotNull BigDecimal usdBrlRate, @NotNull List<@NotNull String> models, @NotNull Boolean estimated,
		@NotNull Boolean cached, @NotNull List<@NotNull StageCostResponse> stages) {
}
