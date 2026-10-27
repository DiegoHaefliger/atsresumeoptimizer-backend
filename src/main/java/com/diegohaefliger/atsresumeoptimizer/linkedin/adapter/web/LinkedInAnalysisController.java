package com.diegohaefliger.atsresumeoptimizer.linkedin.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.linkedin.application.LinkedInAnalysisService;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysisNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/linkedin/analyses")
class LinkedInAnalysisController {

	private final LinkedInAnalysisService service;
	private final LinkedInAnalysisWebMapper mapper;

	LinkedInAnalysisController(LinkedInAnalysisService service, LinkedInAnalysisWebMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	LinkedInAnalysisResponse analyze(@RequestBody(required = false) LinkedInAnalysisRequest request) {
		return mapper.toResponse(service.analyze(request == null ? null : request.snapshotId()));
	}

	@GetMapping
	List<LinkedInAnalysisSummaryResponse> history() {
		return service.history().stream().map(mapper::toSummaryResponse).toList();
	}

	@GetMapping("/latest")
	LinkedInAnalysisResponse latest() {
		return mapper.toResponse(service.latest().orElseThrow(LinkedInAnalysisNotFoundException::new));
	}

	@GetMapping("/{id}")
	LinkedInAnalysisResponse get(@PathVariable UUID id) {
		return mapper.toResponse(service.get(id));
	}
}
