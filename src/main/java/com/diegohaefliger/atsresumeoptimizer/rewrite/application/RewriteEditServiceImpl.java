package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisSnapshot;
import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisSnapshotPort;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ComposedResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeContact;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewriteEditService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Edição é do próprio candidato: sem IA e sem guard de termo inventado, só a mesma limpeza de texto da reescrita. */
@Service
class RewriteEditServiceImpl implements RewriteEditService {

	private final AnalysisSnapshotPort analysisSnapshotPort;
	private final ResumeService resumeService;
	private final ResumeDocumentWriter documentWriter;

	RewriteEditServiceImpl(AnalysisSnapshotPort analysisSnapshotPort, ResumeService resumeService,
			ResumeDocumentWriter documentWriter) {
		this.analysisSnapshotPort = analysisSnapshotPort;
		this.resumeService = resumeService;
		this.documentWriter = documentWriter;
	}

	@Override
	@Transactional
	public ComposedResume saveEdited(
			AnalysisId analysisId, ResumeTemplate template, StructuredResume content, ResumeContact contact) {
		AnalysisSnapshot snapshot =
				analysisSnapshotPort.find(analysisId).orElseThrow(() -> new AnalysisNotFoundException(analysisId));
		StructuredResume sanitized = StructuredResumeSanitizer.sanitizeGrammar(content);
		return resumeService.latestAdaptedVersion(analysisId.value())
				.map(adaptedVersionId -> documentWriter.writeAdaptedVersion(adaptedVersionId, template, sanitized, contact))
				.orElseGet(() -> documentWriter.writeAdapted(snapshot.resumeVersionId(), analysisId.value(),
						AdaptedResumeTitle.suffix(null, snapshot), template, sanitized, contact))
				.toComposed();
	}
}
