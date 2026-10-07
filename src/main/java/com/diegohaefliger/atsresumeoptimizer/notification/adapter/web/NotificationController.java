package com.diegohaefliger.atsresumeoptimizer.notification.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.notification.application.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController {

	private static final int MAX_LIMIT = 200;

	private final NotificationService service;
	private final NotificationWebMapper mapper;

	NotificationController(NotificationService service, NotificationWebMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	List<NotificationResponse> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
			@RequestParam(defaultValue = "50") @Min(1) @Max(MAX_LIMIT) int limit) {
		return service.list(unreadOnly, limit).stream().map(mapper::toResponse).toList();
	}

	@GetMapping("/unread-count")
	UnreadCountResponse unreadCount() {
		return new UnreadCountResponse(service.unreadCount());
	}

	@PostMapping("/{id}/read")
	NotificationResponse markRead(@PathVariable UUID id) {
		return mapper.toResponse(service.markRead(id));
	}

	@PostMapping("/read-all")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void markAllRead() {
		service.markAllRead();
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable UUID id) {
		service.delete(id);
	}
}
