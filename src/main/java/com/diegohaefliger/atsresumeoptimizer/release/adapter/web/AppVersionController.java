package com.diegohaefliger.atsresumeoptimizer.release.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.release.application.UpdateCheckService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/app")
class AppVersionController {

	private final UpdateCheckService updateCheckService;
	private final AppVersionWebMapper mapper;

	AppVersionController(UpdateCheckService updateCheckService, AppVersionWebMapper mapper) {
		this.updateCheckService = updateCheckService;
		this.mapper = mapper;
	}

	@GetMapping("/version")
	AppVersionResponse version() {
		return mapper.toResponse(updateCheckService.status());
	}
}
