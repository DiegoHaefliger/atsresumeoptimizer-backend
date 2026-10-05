package com.diegohaefliger.atsresumeoptimizer.scoring.domain;

public record Finding(
		FindingCode code, Severity severity, String message, String suggestion, Integer startOffset, Integer endOffset,
		String excerpt) {

	public static Finding of(FindingCode code, String message, String suggestion) {
		return new Finding(code, code.defaultSeverity(), message, suggestion, null, null, null);
	}
}
