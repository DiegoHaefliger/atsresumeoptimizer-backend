package com.diegohaefliger.atsresumeoptimizer.linkedin.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.linkedin.application.LinkedInSnapshotService;
import com.diegohaefliger.atsresumeoptimizer.linkedin.application.PairingTokenService;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshot;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshotNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.SnapshotSource;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/linkedin/snapshots")
class LinkedInSnapshotController {

	static final String PAIRING_HEADER = "X-Pairing-Token";

	private final LinkedInSnapshotService snapshotService;
	private final PairingTokenService pairingService;
	private final LinkedInSnapshotWebMapper mapper;

	LinkedInSnapshotController(LinkedInSnapshotService snapshotService, PairingTokenService pairingService,
			LinkedInSnapshotWebMapper mapper) {
		this.snapshotService = snapshotService;
		this.pairingService = pairingService;
		this.mapper = mapper;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	LinkedInSnapshotResponse fromExtension(@RequestHeader(value = PAIRING_HEADER, required = false) String token,
			@Valid @RequestBody LinkedInSnapshotRequest request) {
		pairingService.verify(token);
		return capture(request, SnapshotSource.EXTENSION);
	}

	@PostMapping("/manual")
	@ResponseStatus(HttpStatus.CREATED)
	LinkedInSnapshotResponse fromText(@Valid @RequestBody LinkedInSnapshotRequest request) {
		return capture(request, SnapshotSource.TEXT);
	}

	@GetMapping
	List<LinkedInSnapshotSummaryResponse> history() {
		return snapshotService.history().stream().map(mapper::toSummaryResponse).toList();
	}

	@GetMapping("/latest")
	LinkedInSnapshotResponse latest() {
		return mapper.toResponse(snapshotService.latest().orElseThrow(LinkedInSnapshotNotFoundException::new));
	}

	@GetMapping("/{id}")
	LinkedInSnapshotResponse get(@PathVariable UUID id) {
		return mapper.toResponse(snapshotService.get(id));
	}

	@GetMapping("/{id}/photo")
	ResponseEntity<byte[]> photo(@PathVariable UUID id) {
		LinkedInSnapshot snapshot = snapshotService.get(id);
		return image(snapshot.photo(), snapshot.photoMimeType());
	}

	@GetMapping("/{id}/banner")
	ResponseEntity<byte[]> banner(@PathVariable UUID id) {
		LinkedInSnapshot snapshot = snapshotService.get(id);
		return image(snapshot.banner(), snapshot.bannerMimeType());
	}

	private LinkedInSnapshotResponse capture(LinkedInSnapshotRequest request, SnapshotSource source) {
		return mapper.toResponse(snapshotService.capture(mapper.toCommand(request, source)));
	}

	private static ResponseEntity<byte[]> image(byte[] data, String mimeType) {
		if (data == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(mimeType)).body(data);
	}
}
