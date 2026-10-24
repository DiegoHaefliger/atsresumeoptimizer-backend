package com.diegohaefliger.atsresumeoptimizer.jobsearch.adapter.web;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.JobSuggestionService;

@RestController
@RequestMapping("/api/v1/job-suggestions")
class JobSuggestionController {

	private final JobSuggestionService suggestionService;
	private final JobSearchWebMapper mapper;

	JobSuggestionController(JobSuggestionService suggestionService, JobSearchWebMapper mapper) {
		this.suggestionService = suggestionService;
		this.mapper = mapper;
	}

	@GetMapping
	List<JobSuggestionResponse> newSuggestions() {
		return suggestionService.newSuggestions().stream().map(mapper::toResponse).toList();
	}

	@GetMapping("/below-adherence")
	List<JobSuggestionResponse> belowAdherence() {
		return suggestionService.belowAdherence().stream().map(mapper::toResponse).toList();
	}

	@GetMapping("/discarded")
	List<JobSuggestionResponse> discarded() {
		return suggestionService.discarded().stream().map(mapper::toResponse).toList();
	}

	@GetMapping("/count")
	SuggestionCountResponse count() {
		return new SuggestionCountResponse(suggestionService.countNew());
	}

	@PostMapping("/{id}/promote")
	PromotedJobResponse promote(@PathVariable UUID id) {
		return new PromotedJobResponse(suggestionService.promote(id));
	}

	@PostMapping("/{id}/discard")
	JobSuggestionResponse discard(@PathVariable UUID id) {
		return mapper.toResponse(suggestionService.discard(id));
	}

	@PostMapping("/{id}/restore")
	JobSuggestionResponse restore(@PathVariable UUID id) {
		return mapper.toResponse(suggestionService.restore(id));
	}
}
