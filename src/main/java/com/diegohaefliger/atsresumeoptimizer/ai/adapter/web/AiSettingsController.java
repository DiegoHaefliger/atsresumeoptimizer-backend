package com.diegohaefliger.atsresumeoptimizer.ai.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.ai.application.AiConnectionTestResult;
import com.diegohaefliger.atsresumeoptimizer.ai.application.AiModelList;
import com.diegohaefliger.atsresumeoptimizer.ai.application.AiSettingsService;
import com.diegohaefliger.atsresumeoptimizer.ai.application.AiSettingsView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai-settings")
class AiSettingsController {

	private final AiSettingsService settingsService;
	private final AiSettingsRequestMapper mapper;

	AiSettingsController(AiSettingsService settingsService, AiSettingsRequestMapper mapper) {
		this.settingsService = settingsService;
		this.mapper = mapper;
	}

	@GetMapping
	AiSettingsView get() {
		return settingsService.current();
	}

	@PutMapping
	AiSettingsView save(@Valid @RequestBody AiSettingsRequest request) {
		return settingsService.save(mapper.toUpdate(request));
	}

	@PostMapping("/models")
	AiModelList models(@Valid @RequestBody AiModelListRequest request) {
		return settingsService.listModels(mapper.toQuery(request));
	}

	@PostMapping("/test")
	AiConnectionTestResult test(@Valid @RequestBody AiConnectionTestRequest request) {
		return settingsService.test(mapper.toTestCommand(request));
	}
}
