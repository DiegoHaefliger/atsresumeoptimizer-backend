package com.diegohaefliger.atsresumeoptimizer.analysis.adapter.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

record AnalysisFeedbackRequest(@Min(1) @Max(5) int rating, String comment) {
}
