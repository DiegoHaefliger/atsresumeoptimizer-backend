package com.diegohaefliger.atsresumeoptimizer.googlecalendar.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.googlecalendar.application.GoogleCalendarService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

	@PutMapping("/credentials")
	GoogleStatusResponse saveCredentials(@Valid @RequestBody GoogleCredentialsRequest request) {
		return mapper.toResponse(service.saveCredentials(mapper.toCredentials(request)));
	}

	@DeleteMapping("/credentials")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void removeCredentials() {
		service.removeCredentials();
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
