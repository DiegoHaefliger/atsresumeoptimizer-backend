package com.diegohaefliger.atsresumeoptimizer.rewrite.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewriteEditService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewriteProgressService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewriteQueryService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewriteService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.JobHighlight;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analyses/{id}/rewrite")
class RewriteController {

	private final RewriteService rewriteService;
	private final RewriteEditService rewriteEditService;
	private final RewriteProgressService rewriteProgressService;
	private final RewriteQueryService rewriteQueryService;
	private final RewriteWebMapper mapper;

	RewriteController(RewriteService rewriteService, RewriteEditService rewriteEditService,
			RewriteProgressService rewriteProgressService, RewriteQueryService rewriteQueryService, RewriteWebMapper mapper) {
		this.rewriteService = rewriteService;
		this.rewriteEditService = rewriteEditService;
		this.rewriteProgressService = rewriteProgressService;
		this.rewriteQueryService = rewriteQueryService;
		this.mapper = mapper;
	}

	@PostMapping
	RewriteResultView rewrite(@PathVariable UUID id, @RequestBody(required = false) RewriteRequest request) {
		ResumeTemplate template = request != null && request.template() != null ? request.template() : ResumeTemplate.CLASSIC;
		return mapper.toView(rewriteService.rewrite(new AnalysisId(id), template, jobHighlight(request)));
	}

	@GetMapping
	ResponseEntity<RewriteResultView> latest(@PathVariable UUID id) {
		return rewriteQueryService.latest(new AnalysisId(id))
				.map(mapper::toView)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@GetMapping("/progress")
	RewriteProgressView progress(@PathVariable UUID id) {
		return new RewriteProgressView(rewriteProgressService.currentPhase(new AnalysisId(id)).orElse(null));
	}

	@PostMapping("/documents")
	EditedDocumentsView saveEdited(@PathVariable UUID id, @Valid @RequestBody EditedDocumentRequest request) {
		return mapper.toView(
				rewriteEditService.saveEdited(new AnalysisId(id), request.template(), request.content(), request.contact()));
	}

	private JobHighlight jobHighlight(RewriteRequest request) {
		if (request == null || request.highlightJob() == null) {
			return JobHighlight.AUTO;
		}
		return request.highlightJob() ? JobHighlight.ENABLED : JobHighlight.DISABLED;
	}
}
