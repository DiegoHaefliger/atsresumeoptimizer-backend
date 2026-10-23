package com.diegohaefliger.atsresumeoptimizer.cost.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.ai.AiStage;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

record StageCostResponse(@NotNull AiStage stage, @NotNull Boolean executed, @NotNull Long tokensIn,
		@NotNull Long tokensOut, @NotNull BigDecimal costUsd, @NotNull BigDecimal costBrl,
		@NotNull List<@NotNull String> models, @NotNull Boolean estimated, @NotNull Boolean cached) {
}
