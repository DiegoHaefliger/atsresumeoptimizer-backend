package com.diegohaefliger.atsresumeoptimizer.diagnostics.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.diagnostics.application.DiagnosticsService;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.domain.DiagnosticBundle;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/diagnostics")
class DiagnosticsController {

	private static final MediaType ZIP = MediaType.parseMediaType("application/zip");

	private final DiagnosticsService service;
	private final DiagnosticsWebMapper mapper;

	DiagnosticsController(DiagnosticsService service, DiagnosticsWebMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	DiagnosticReportResponse report() {
		return mapper.toResponse(service.report());
	}

	@GetMapping("/bundle")
	ResponseEntity<byte[]> bundle() {
		DiagnosticBundle bundle = service.bundle();
		return ResponseEntity.ok()
				.contentType(ZIP)
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(bundle.fileName()).build().toString())
				.body(bundle.content());
	}

	@PostMapping("/client-errors")
	@ResponseStatus(HttpStatus.CREATED)
	ClientErrorResponse reportClientError(@Valid @RequestBody ClientErrorRequest request) {
		return new ClientErrorResponse(service.recordClientError(mapper.toDomain(request)));
	}

	@PostMapping("/logs-folder")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void openLogsFolder() {
		service.openLogsFolder();
	}
}
