package com.diegohaefliger.atsresumeoptimizer.cost.domain;

import com.diegohaefliger.atsresumeoptimizer.ai.AiStage;
import java.math.BigDecimal;
import java.util.List;

public record StageCost(AiStage stage, boolean executed, long tokensIn, long tokensOut, BigDecimal costUsd,
		BigDecimal costBrl, List<String> models, boolean estimated, boolean cached) {
}
