package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import java.util.List;

public record ScoreSummaryView(Integer overall, List<DimensionView> dimensions) {
}
