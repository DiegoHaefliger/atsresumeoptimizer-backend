package com.diegohaefliger.atsresumeoptimizer.careerprofile.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.careerprofile.application.CareerProfileService;
import com.diegohaefliger.atsresumeoptimizer.careerprofile.domain.CareerProfileNotFoundException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/career-profile")
class CareerProfileController {

	private final CareerProfileService service;
	private final CareerProfileWebMapper mapper;

	CareerProfileController(CareerProfileService service, CareerProfileWebMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	CareerProfileResponse get() {
		return service.current().map(mapper::toResponse).orElseThrow(CareerProfileNotFoundException::new);
	}

	@PutMapping
	CareerProfileResponse save(@Valid @RequestBody CareerProfileRequest request) {
		return mapper.toResponse(service.save(mapper.toDomain(request)));
	}
}
