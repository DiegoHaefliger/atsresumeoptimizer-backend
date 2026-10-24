package com.diegohaefliger.atsresumeoptimizer.jobsearch;

import java.util.List;
import java.util.Optional;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SearchRun;

public interface JobSearchRunService {

	void startManual();

	void startIfDue();

	boolean running();

	Optional<SearchRun> lastRun();

	List<SearchRun> history();
}
