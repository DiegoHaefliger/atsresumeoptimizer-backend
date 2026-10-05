package com.diegohaefliger.atsresumeoptimizer.job.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.job.JobRegistration;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
class JobsController {

	private final JobStructuringService jobStructuringService;
	private final CreateJobRequestMapper requestMapper;

	JobsController(JobStructuringService jobStructuringService, CreateJobRequestMapper requestMapper) {
		this.jobStructuringService = jobStructuringService;
		this.requestMapper = requestMapper;
	}

	@PostMapping
	ResponseEntity<JobCreatedResponse> create(@Valid @RequestBody CreateJobRequest request) {
		JobRegistration registration =
				jobStructuringService.registerAndStructure(request.text(), requestMapper.toJobDetails(request));
		return ResponseEntity.status(HttpStatus.CREATED).body(requestMapper.toResponse(registration));
	}

	@PutMapping("/{id}")
	JobCreatedResponse update(@PathVariable UUID id, @Valid @RequestBody CreateJobRequest request) {
		JobRegistration registration =
				jobStructuringService.update(id, request.text(), requestMapper.toJobDetails(request));
		return requestMapper.toResponse(registration);
	}
}
