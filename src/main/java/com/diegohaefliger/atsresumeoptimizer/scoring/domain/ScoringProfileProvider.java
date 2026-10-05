package com.diegohaefliger.atsresumeoptimizer.scoring.domain;

/** Pesos calibráveis sem deploy: vêm do {@code scoring_profile} ativo no banco. */
public interface ScoringProfileProvider {

	ScoringProfileLookup activeProfile(AnalysisMode mode);
}
