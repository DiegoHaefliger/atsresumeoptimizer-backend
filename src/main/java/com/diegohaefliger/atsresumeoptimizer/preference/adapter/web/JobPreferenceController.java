package com.diegohaefliger.atsresumeoptimizer.preference.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.preference.application.JobPreferenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/job-preference")
class JobPreferenceController {

	private final JobPreferenceService preferenceService;
	private final JobPreferenceWebMapper mapper;

	JobPreferenceController(JobPreferenceService preferenceService, JobPreferenceWebMapper mapper) {
		this.preferenceService = preferenceService;
		this.mapper = mapper;
	}

	@GetMapping
	JobPreferenceResponse get() {
		return mapper.toResponse(preferenceService.current());
	}

	@PutMapping
	JobPreferenceResponse save(@Valid @RequestBody JobPreferenceRequest request) {
		return mapper.toResponse(preferenceService.save(mapper.toDomain(request)));
	}
}
