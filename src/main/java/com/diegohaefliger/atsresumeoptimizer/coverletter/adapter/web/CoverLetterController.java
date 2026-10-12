package com.diegohaefliger.atsresumeoptimizer.coverletter.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.coverletter.application.CoverLetterService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs/{jobId}/cover-letters")
class CoverLetterController {

	private final CoverLetterService service;
	private final CoverLetterWebMapper mapper;

	CoverLetterController(CoverLetterService service, CoverLetterWebMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	List<CoverLetterResponse> list(@PathVariable UUID jobId) {
		return mapper.toResponses(service.list(jobId));
	}

	@PostMapping
	CoverLetterResponse generate(@PathVariable UUID jobId, @Valid @RequestBody CoverLetterGenerationRequest request) {
		return mapper.toResponse(service.generate(jobId, request.resumeVersionId()));
	}

	@PutMapping("/{id}")
	CoverLetterResponse update(@PathVariable UUID jobId, @PathVariable UUID id, @Valid @RequestBody CoverLetterRequest request) {
		return mapper.toResponse(service.update(jobId, id, request.content()));
	}
}
