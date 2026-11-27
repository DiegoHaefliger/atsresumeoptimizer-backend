package com.diegohaefliger.atsresumeoptimizer.jobsearch.adapter.web;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
	JobSuggestionPageResponse newSuggestions(@Valid @ParameterObject @ModelAttribute SuggestionPageRequest request) {
		return mapper.toResponse(suggestionService.newSuggestions(mapper.toQuery(request)));
	}

	@GetMapping("/below-adherence")
	JobSuggestionPageResponse belowAdherence(@Valid @ParameterObject @ModelAttribute SuggestionPageRequest request) {
		return mapper.toResponse(suggestionService.belowAdherence(mapper.toQuery(request)));
	}

	@GetMapping("/discarded")
	JobSuggestionPageResponse discarded(@Valid @ParameterObject @ModelAttribute SuggestionPageRequest request) {
		return mapper.toResponse(suggestionService.discarded(mapper.toQuery(request)));
	}

	@GetMapping("/count")
	SuggestionCountResponse count() {
		return mapper.toResponse(suggestionService.counts());
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
