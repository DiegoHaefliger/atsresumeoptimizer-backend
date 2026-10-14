package com.diegohaefliger.atsresumeoptimizer.jobnote.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.jobnote.application.JobNoteService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs/{jobId}/note")
class JobNoteController {

	private final JobNoteService service;
	private final JobNoteWebMapper mapper;

	JobNoteController(JobNoteService service, JobNoteWebMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	JobNoteResponse get(@PathVariable UUID jobId) {
		return mapper.toResponse(service.get(jobId));
	}

	@PutMapping
	JobNoteResponse save(@PathVariable UUID jobId, @Valid @RequestBody JobNoteRequest request) {
		return mapper.toResponse(service.save(jobId, request.content()));
	}
}
