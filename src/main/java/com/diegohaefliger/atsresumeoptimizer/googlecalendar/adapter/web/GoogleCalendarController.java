package com.diegohaefliger.atsresumeoptimizer.googlecalendar.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.googlecalendar.application.GoogleCalendarService;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/google-calendar")
class GoogleCalendarController {

	private final GoogleCalendarService service;
	private final GoogleCalendarWebMapper mapper;

	GoogleCalendarController(GoogleCalendarService service, GoogleCalendarWebMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	GoogleStatusResponse status() {
		return mapper.toResponse(service.status());
	}

	@GetMapping("/events")
	List<GoogleEventResponse> events(@RequestParam Instant from, @RequestParam Instant to) {
		return service.events(from, to).stream().map(mapper::toResponse).toList();
	}

	@GetMapping("/authorize")
	GoogleAuthorizationResponse authorize() {
		return new GoogleAuthorizationResponse(service.authorizationUrl());
	}

	@GetMapping("/callback")
	ResponseEntity<Void> callback(
			@RequestParam(required = false) String code, @RequestParam(required = false) String state) {
		String target = service.completeAuthorization(code, state);
		return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, URI.create(target).toString()).build();
	}

	@PostMapping("/sync")
	GoogleSyncResponse sync() {
		return new GoogleSyncResponse(service.syncAll());
	}

	@DeleteMapping("/connection")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void disconnect() {
		service.disconnect();
	}
}
