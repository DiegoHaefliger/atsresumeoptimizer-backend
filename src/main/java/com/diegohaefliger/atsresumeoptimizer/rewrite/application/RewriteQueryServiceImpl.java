package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisSnapshot;
import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisSnapshotPort;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeSummary;
import com.diegohaefliger.atsresumeoptimizer.rewrite.EditableResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewriteQueryService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewriteResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RewriteQueryServiceImpl implements RewriteQueryService {

	private final AnalysisSnapshotPort analysisSnapshots;
	private final ResumeService resumeService;
	private final ResumeContentStore contentStore;
	private final RewriteRepository rewriteRepository;

	RewriteQueryServiceImpl(AnalysisSnapshotPort analysisSnapshots, ResumeService resumeService,
			ResumeContentStore contentStore, RewriteRepository rewriteRepository) {
		this.analysisSnapshots = analysisSnapshots;
		this.resumeService = resumeService;
		this.contentStore = contentStore;
		this.rewriteRepository = rewriteRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<RewriteResult> latest(AnalysisId analysisId) {
		return analysisSnapshots.find(analysisId)
				.flatMap(snapshot -> resumeService.latestAdaptedVersion(analysisId.value())
						.flatMap(contentStore::findSaved)
						.map(saved -> toResult(analysisId, snapshot, saved)));
	}

	private RewriteResult toResult(AnalysisId analysisId, AnalysisSnapshot snapshot, SavedResumeContent saved) {
		StoredResumeDocuments documents = saved.documents();
		EditableResume editable = saved.editable();
		Optional<RewriteEntity> rewrite = rewriteRepository.findFirstByAnalysisIdOrderByCreatedAtDesc(analysisId.value());
		return new RewriteResult(documents.resumeId(), documents.docxVersionId(), documents.pdfVersionId(), List.of(),
				snapshot.overallScore(), atsScore(analysisId, documents.resumeId()),
				rewrite.map(RewriteEntity::jobHighlighted).orElse(false), List.of(), editable.content(), editable.contact(),
				List.of(), List.of(), null, rewrite.map(RewriteEntity::aiModel).orElse(null), editable.template());
	}

	private Integer atsScore(AnalysisId analysisId, UUID resumeId) {
		return resumeService.listAdaptedFromAnalyses(List.of(analysisId.value())).stream()
				.filter(resume -> resume.id().equals(resumeId))
				.map(ResumeSummary::atsScore)
				.findFirst()
				.orElse(null);
	}
}
