package com.diegohaefliger.atsresumeoptimizer.analysis.domain;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;

public record AnalysisId(UUID value) {

	public static AnalysisId generate() {
		return new AnalysisId(UuidCreator.getTimeOrderedEpoch());
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
