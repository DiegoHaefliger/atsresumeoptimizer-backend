package com.diegohaefliger.atsresumeoptimizer.jobsearch;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SearchProgress;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SearchRun;

public interface JobSearchRunService {

	void startManual();

	void startIfDue();

	boolean running();

	SearchProgress progress();

	Optional<Instant> nextRun();

	Optional<SearchRun> lastRun();

	List<SearchRun> history();
}
