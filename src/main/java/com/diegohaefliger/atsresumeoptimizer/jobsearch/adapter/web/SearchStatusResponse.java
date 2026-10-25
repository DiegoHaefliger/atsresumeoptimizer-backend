package com.diegohaefliger.atsresumeoptimizer.jobsearch.adapter.web;

import java.time.Instant;

record SearchStatusResponse(boolean running, SearchRunResponse lastRun, Instant nextRun,
		SearchProgressResponse progress) {
}
