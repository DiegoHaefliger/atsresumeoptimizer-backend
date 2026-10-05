package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import java.math.BigDecimal;
import java.util.List;

public record CostReport(long analysesWithAiUsage, BigDecimal totalCostUsd, BigDecimal avgCostUsd, List<CostByModel> byModel) {
}
