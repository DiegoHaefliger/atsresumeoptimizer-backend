package com.diegohaefliger.atsresumeoptimizer.scoring.domain;

public interface DimensionScorer {

	Dimension dimension();

	DimensionResult score(ScoringContext context);
}
