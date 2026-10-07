package com.diegohaefliger.atsresumeoptimizer.coverletter.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.coverletter.application.CoverLetterService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analyses/{id}/cover-letter")
class CoverLetterController {

	private final CoverLetterService service;
	private final CoverLetterWebMapper mapper;

	CoverLetterController(CoverLetterService service, CoverLetterWebMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	ResponseEntity<CoverLetterResponse> find(@PathVariable UUID id) {
		return service.find(new AnalysisId(id))
				.map(mapper::toResponse)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@PostMapping
	CoverLetterResponse generate(@PathVariable UUID id) {
		return mapper.toResponse(service.generate(new AnalysisId(id)));
	}

	@PutMapping
	CoverLetterResponse save(@PathVariable UUID id, @Valid @RequestBody CoverLetterRequest request) {
		return mapper.toResponse(service.save(new AnalysisId(id), request.content()));
	}
}
