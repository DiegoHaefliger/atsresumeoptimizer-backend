package com.diegohaefliger.atsresumeoptimizer.jobsearch.domain;

import java.time.Instant;
import java.util.UUID;

public record SearchRun(
		UUID id,
		RunTrigger trigger,
		Instant startedAt,
		Instant finishedAt,
		int read,
		int added,
		int duplicates,
		int belowAdherence,
		String error) {
}
