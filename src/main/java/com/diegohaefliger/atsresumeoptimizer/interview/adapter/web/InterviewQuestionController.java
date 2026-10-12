package com.diegohaefliger.atsresumeoptimizer.interview.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.interview.application.InterviewQuestionService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs/{jobId}/interview-questions")
class InterviewQuestionController {

	private final InterviewQuestionService service;
	private final InterviewQuestionWebMapper mapper;

	InterviewQuestionController(InterviewQuestionService service, InterviewQuestionWebMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	List<InterviewQuestionSetResponse> list(@PathVariable UUID jobId) {
		return mapper.toResponses(service.list(jobId));
	}

	@PostMapping
	InterviewQuestionSetResponse generate(@PathVariable UUID jobId, @RequestBody InterviewQuestionsRequest request) {
		return mapper.toResponse(service.generate(jobId, request.resumeVersionId()));
	}
}
