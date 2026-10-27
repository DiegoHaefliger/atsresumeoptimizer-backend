package com.diegohaefliger.atsresumeoptimizer.linkedin.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.linkedin.application.PairingTokenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/linkedin/pairing-token")
class PairingTokenController {

	private final PairingTokenService service;

	PairingTokenController(PairingTokenService service) {
		this.service = service;
	}

	@GetMapping
	PairingTokenResponse current() {
		return new PairingTokenResponse(service.current());
	}

	@PostMapping("/regenerate")
	PairingTokenResponse regenerate() {
		return new PairingTokenResponse(service.regenerate());
	}
}
