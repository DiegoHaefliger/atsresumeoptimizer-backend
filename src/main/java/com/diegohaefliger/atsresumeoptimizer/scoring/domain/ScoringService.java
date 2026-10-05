package com.diegohaefliger.atsresumeoptimizer.scoring.domain;

public interface ScoringService {

	ScoringOutcome score(ScoringProfile profile, ScoringContext context);
}
