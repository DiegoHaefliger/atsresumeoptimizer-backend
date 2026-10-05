package com.diegohaefliger.atsresumeoptimizer.scoring.domain;

import java.util.List;

public record ScoringOutcome(int overallScore, List<DimensionResult> dimensions, List<FindingCode> blockers) {

	public ScoringOutcome {
		dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
		blockers = blockers == null ? List.of() : List.copyOf(blockers);
	}
}
