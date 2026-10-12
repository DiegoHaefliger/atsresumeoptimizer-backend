package com.diegohaefliger.atsresumeoptimizer.candidatequestion.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.candidatequestion.application.CandidateQuestionService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs/{jobId}/candidate-questions")
class CandidateQuestionController {

	private final CandidateQuestionService service;
	private final CandidateQuestionWebMapper mapper;

	CandidateQuestionController(CandidateQuestionService service, CandidateQuestionWebMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	List<CandidateQuestionSetResponse> list(@PathVariable UUID jobId) {
		return mapper.toResponses(service.list(jobId));
	}

	@PostMapping
	CandidateQuestionSetResponse generate(@PathVariable UUID jobId) {
		return mapper.toResponse(service.generate(jobId));
	}
}
