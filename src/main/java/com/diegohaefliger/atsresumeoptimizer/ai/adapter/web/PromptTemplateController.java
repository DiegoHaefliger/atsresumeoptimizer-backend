package com.diegohaefliger.atsresumeoptimizer.ai.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.ai.application.PromptTemplateService;
import com.diegohaefliger.atsresumeoptimizer.ai.application.PromptTemplateSummary;
import com.diegohaefliger.atsresumeoptimizer.ai.application.PromptTemplateView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prompt-templates")
class PromptTemplateController {

	private final PromptTemplateService promptTemplateService;
	private final PromptTemplateRequestMapper mapper;

	PromptTemplateController(PromptTemplateService promptTemplateService, PromptTemplateRequestMapper mapper) {
		this.promptTemplateService = promptTemplateService;
		this.mapper = mapper;
	}

	@GetMapping
	List<PromptTemplateSummary> list() {
		return promptTemplateService.listLatest();
	}

	@GetMapping("/{key}")
	PromptTemplateView current(@PathVariable String key) {
		return promptTemplateService.current(key);
	}

	@GetMapping("/{key}/versions")
	List<PromptTemplateSummary> history(@PathVariable String key) {
		return promptTemplateService.history(key);
	}

	@GetMapping("/{key}/versions/{version}")
	PromptTemplateView version(@PathVariable String key, @PathVariable int version) {
		return promptTemplateService.version(key, version);
	}

	@PutMapping("/{key}")
	PromptTemplateView publish(@PathVariable String key, @Valid @RequestBody PromptTemplateRequest request) {
		return promptTemplateService.publish(key, mapper.toUpdate(request));
	}
}
