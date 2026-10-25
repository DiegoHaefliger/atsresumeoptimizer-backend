package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.JobSearchRunService;

@Component
class JobSearchScheduler {

	private final JobSearchRunService runService;

	JobSearchScheduler(JobSearchRunService runService) {
		this.runService = runService;
	}

	@Scheduled(fixedDelayString = "${app.job-search.check-interval:PT1M}",
			initialDelayString = "${app.job-search.initial-delay:PT1M}")
	void runWhenDue() {
		runService.startIfDue();
	}
}
