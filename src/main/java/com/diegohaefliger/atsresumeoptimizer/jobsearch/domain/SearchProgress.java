package com.diegohaefliger.atsresumeoptimizer.jobsearch.domain;

public record SearchProgress(int read, int added, int belowAdherence) {

	public static final SearchProgress NONE = new SearchProgress(0, 0, 0);
}
