package com.diegohaefliger.atsresumeoptimizer.scoring.domain;

import java.util.List;

public record DimensionResult(Dimension dimension, int score, List<Finding> findings, List<KeywordMatch> keywordMatches) {

	public DimensionResult {
		findings = findings == null ? List.of() : List.copyOf(findings);
		keywordMatches = keywordMatches == null ? List.of() : List.copyOf(keywordMatches);
	}

	public static DimensionResult of(Dimension dimension, int score, List<Finding> findings) {
		return new DimensionResult(dimension, score, findings, List.of());
	}
}
