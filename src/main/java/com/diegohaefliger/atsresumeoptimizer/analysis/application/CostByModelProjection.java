package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import java.math.BigDecimal;

interface CostByModelProjection {

	String getModel();

	long getAnalysisCount();

	BigDecimal getTotalCostUsd();

	BigDecimal getAvgCostUsd();
}
