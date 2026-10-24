package com.diegohaefliger.atsresumeoptimizer.jobsearch.adapter.web;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.JobSearchRunService;

@RestController
@RequestMapping("/api/v1/job-search")
class JobSearchController {

	private final JobSearchRunService runService;
	private final JobSearchWebMapper mapper;

	JobSearchController(JobSearchRunService runService, JobSearchWebMapper mapper) {
		this.runService = runService;
		this.mapper = mapper;
	}

	@PostMapping("/run")
	@ResponseStatus(HttpStatus.ACCEPTED)
	void run() {
		runService.startManual();
	}

	@GetMapping("/status")
	SearchStatusResponse status() {
		return new SearchStatusResponse(runService.running(), runService.lastRun().map(mapper::toResponse).orElse(null));
	}

	@GetMapping("/runs")
	List<SearchRunResponse> runs() {
		return runService.history().stream().map(mapper::toResponse).toList();
	}
}
