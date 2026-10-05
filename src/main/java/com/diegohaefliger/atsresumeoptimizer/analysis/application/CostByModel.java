package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import java.math.BigDecimal;

public record CostByModel(String model, long analysisCount, BigDecimal totalCostUsd, BigDecimal avgCostUsd) {
}
