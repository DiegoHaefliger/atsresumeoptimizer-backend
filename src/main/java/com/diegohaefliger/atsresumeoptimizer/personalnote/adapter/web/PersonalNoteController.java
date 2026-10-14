package com.diegohaefliger.atsresumeoptimizer.personalnote.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.personalnote.application.PersonalNoteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/personal-note")
class PersonalNoteController {

	private final PersonalNoteService service;
	private final PersonalNoteWebMapper mapper;

	PersonalNoteController(PersonalNoteService service, PersonalNoteWebMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	PersonalNoteResponse get() {
		return mapper.toResponse(service.get());
	}

	@PutMapping
	PersonalNoteResponse save(@Valid @RequestBody PersonalNoteRequest request) {
		return mapper.toResponse(service.save(request.content()));
	}
}
