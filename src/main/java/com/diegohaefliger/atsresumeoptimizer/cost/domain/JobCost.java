package com.diegohaefliger.atsresumeoptimizer.cost.domain;

import java.math.BigDecimal;
import java.util.List;

public record JobCost(BigDecimal totalCostUsd, BigDecimal totalCostBrl, BigDecimal usdBrlRate, List<String> models,
		boolean estimated, boolean cached, List<StageCost> stages) {
}
