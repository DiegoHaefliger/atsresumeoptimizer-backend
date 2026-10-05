package com.diegohaefliger.atsresumeoptimizer.rewrite.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeCompositionService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeEditingService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ExportedResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeFormat;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resumes")
class ResumeCompositionController {

	private final ResumeCompositionService compositionService;
	private final ResumeEditingService editingService;
	private final RewriteWebMapper mapper;

	ResumeCompositionController(ResumeCompositionService compositionService, ResumeEditingService editingService,
			RewriteWebMapper mapper) {
		this.compositionService = compositionService;
		this.editingService = editingService;
		this.mapper = mapper;
	}

	@PostMapping("/preview")
	ResponseEntity<byte[]> preview(@Valid @RequestBody EditedDocumentRequest request) {
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.body(compositionService.previewPdf(request.template(), request.content(), request.contact()));
	}

	@PostMapping("/composed")
	@ResponseStatus(HttpStatus.CREATED)
	EditedDocumentsView compose(@Valid @RequestBody EditedDocumentRequest request) {
		return mapper.toView(compositionService.compose(request.template(), request.content(), request.contact()));
	}

	@GetMapping("/{resumeId}/versions/{versionId}/editable")
	EditableResumeView editable(@PathVariable UUID resumeId, @PathVariable UUID versionId) {
		return mapper.toView(editingService.load(resumeId, versionId));
	}

	@GetMapping("/{resumeId}/versions/{versionId}/export")
	ResponseEntity<byte[]> export(
			@PathVariable UUID resumeId, @PathVariable UUID versionId, @RequestParam ResumeFormat format) {
		ExportedResume exported = editingService.export(resumeId, versionId, format);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(exported.mimeType()))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(exported.fileName()).build().toString())
				.body(exported.content());
	}

	@PutMapping("/{resumeId}/versions/{versionId}/editable")
	EditedDocumentsView saveEdited(
			@PathVariable UUID resumeId, @PathVariable UUID versionId, @Valid @RequestBody EditedDocumentRequest request) {
		return mapper.toView(
				editingService.save(resumeId, versionId, request.title(), request.template(), request.content(),
						request.contact()));
	}
}
