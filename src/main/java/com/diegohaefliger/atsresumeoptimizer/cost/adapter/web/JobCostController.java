package com.diegohaefliger.atsresumeoptimizer.cost.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.cost.application.JobCostService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs/{jobId}/cost")
class JobCostController {

	private final JobCostService service;
	private final JobCostWebMapper mapper;

	JobCostController(JobCostService service, JobCostWebMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	JobCostResponse cost(@PathVariable UUID jobId) {
		return mapper.toResponse(service.cost(jobId));
	}
}
