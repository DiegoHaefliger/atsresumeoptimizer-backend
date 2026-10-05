package com.diegohaefliger.atsresumeoptimizer.analysis.application;

public record FindingView(
		String code, String severity, String dimension, String message, String suggestion, OffsetRange range) {
}
