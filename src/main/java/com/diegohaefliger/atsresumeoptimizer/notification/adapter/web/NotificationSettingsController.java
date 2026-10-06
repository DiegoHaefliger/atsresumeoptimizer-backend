package com.diegohaefliger.atsresumeoptimizer.notification.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.notification.application.NotificationSettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notification-settings")
class NotificationSettingsController {

	private final NotificationSettingsService service;
	private final NotificationSettingsWebMapper mapper;

	NotificationSettingsController(NotificationSettingsService service, NotificationSettingsWebMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	NotificationSettingsResponse get() {
		return mapper.toResponse(service.current());
	}

	@PutMapping
	NotificationSettingsResponse save(@Valid @RequestBody NotificationSettingsRequest request) {
		return mapper.toResponse(service.save(mapper.toDomain(request)));
	}
}
