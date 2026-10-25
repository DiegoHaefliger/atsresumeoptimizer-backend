package com.diegohaefliger.atsresumeoptimizer.jobsearch.adapter.web;

import java.time.Instant;
import java.util.UUID;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.RunTrigger;

record SearchRunResponse(
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
