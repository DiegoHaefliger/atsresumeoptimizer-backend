package com.diegohaefliger.atsresumeoptimizer.jobsearch.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.jobsearch.PostImportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/job-suggestions/import")
class PostImportController {

	private final PostImportService importService;
	private final JobSearchWebMapper mapper;

	PostImportController(PostImportService importService, JobSearchWebMapper mapper) {
		this.importService = importService;
		this.mapper = mapper;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	JobSuggestionResponse importPost(@Valid @RequestBody ImportPostRequest request) {
		return mapper.toResponse(importService.importPost(request.url(), request.text()));
	}
}
