package com.diegohaefliger.atsresumeoptimizer.scoring.domain;

import java.util.Map;

public final class ScoringProfileV1 {

	public static final ScoringProfile JOB_MATCH = new ScoringProfile(
		"default",
		1,
		AnalysisMode.JOB_MATCH,
		Map.of(
			Dimension.PARSEABILITY, 0.20,
			Dimension.STRUCTURE_SECTIONS, 0.10,
			Dimension.CONTACT_DATA, 0.05,
			Dimension.KEYWORD_MATCH, 0.30,
			Dimension.REQUIREMENTS_SENIORITY, 0.15,
			Dimension.CONTENT_QUALITY, 0.10,
			Dimension.LANGUAGE, 0.10
		)
	);

	public static final ScoringProfile GENERAL = new ScoringProfile(
		"default",
		1,
		AnalysisMode.GENERAL,
		Map.of(
			Dimension.PARSEABILITY, 0.25,
			Dimension.STRUCTURE_SECTIONS, 0.15,
			Dimension.CONTACT_DATA, 0.10,
			Dimension.CONTENT_QUALITY, 0.35,
			Dimension.LANGUAGE, 0.15
		)
	);

	private ScoringProfileV1() {
	}
}
