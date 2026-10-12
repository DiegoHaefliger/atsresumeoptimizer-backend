package com.diegohaefliger.atsresumeoptimizer.desktop.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.desktop.application.DesktopDatabaseService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("desktop")
@RequestMapping("/api/v1/desktop/database")
class DesktopDatabaseController {

	private final DesktopDatabaseService databaseService;
	private final DesktopDatabaseWebMapper mapper;

	DesktopDatabaseController(DesktopDatabaseService databaseService, DesktopDatabaseWebMapper mapper) {
		this.databaseService = databaseService;
		this.mapper = mapper;
	}

	@GetMapping
	DatabaseSettingsResponse current() {
		return mapper.toResponse(databaseService.current());
	}

	@PostMapping("/test")
	ConnectionCheckResponse test(@Valid @RequestBody ExternalDatabaseRequest request) {
		return mapper.toResponse(databaseService.check(mapper.toDomain(request)));
	}

	@PutMapping
	DatabaseSwitchResponse useExternal(@Valid @RequestBody ExternalDatabaseRequest request) {
		return new DatabaseSwitchResponse(databaseService.useExternal(mapper.toDomain(request), request.copyData()));
	}

	@DeleteMapping
	DatabaseSwitchResponse useEmbedded() {
		return new DatabaseSwitchResponse(databaseService.useEmbedded());
	}
}
