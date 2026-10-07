package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisSnapshot;
import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisSnapshotPort;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisStatus;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeOrigin;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeSummary;
import com.diegohaefliger.atsresumeoptimizer.rewrite.EditableResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeContact;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewriteResult;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RewriteQueryServiceImplTest {

	private static final AnalysisId ANALYSIS_ID = new AnalysisId(UUID.randomUUID());
	private static final UUID RESUME_ID = UUID.randomUUID();
	private static final UUID DOCX_ID = UUID.randomUUID();
	private static final UUID PDF_ID = UUID.randomUUID();

	@Mock
	private AnalysisSnapshotPort analysisSnapshots;
	@Mock
	private ResumeService resumeService;
	@Mock
	private ResumeContentStore contentStore;
	@Mock
	private RewriteRepository rewriteRepository;

	@InjectMocks
	private RewriteQueryServiceImpl service;

	@Test
	void rebuildsTheLatestAdaptationFromWhatWasSaved() {
		StructuredResume content = new StructuredResume("Ana Silva", "Backend", List.of());
		ResumeContact contact = new ResumeContact("ana@email.com", null, null, null, null, null);
		when(analysisSnapshots.find(ANALYSIS_ID)).thenReturn(Optional.of(snapshot()));
		when(resumeService.latestAdaptedVersion(ANALYSIS_ID.value())).thenReturn(Optional.of(PDF_ID));
		when(contentStore.findSaved(PDF_ID)).thenReturn(Optional.of(new SavedResumeContent(
				new StoredResumeDocuments(RESUME_ID, DOCX_ID, PDF_ID, null),
				new EditableResume(null, ResumeTemplate.MODERN_BLUE, content, contact, false))));
		when(rewriteRepository.findFirstByAnalysisIdOrderByCreatedAtDesc(ANALYSIS_ID.value())).thenReturn(Optional.of(
				new RewriteEntity(UUID.randomUUID(), ANALYSIS_ID.value(), DOCX_ID, PDF_ID, "gpt-4o-mini", 1, 1,
						BigDecimal.ZERO, true)));
		when(resumeService.listAdaptedFromAnalyses(List.of(ANALYSIS_ID.value()))).thenReturn(List.of(new ResumeSummary(
				RESUME_ID, "Ana · adaptado", Instant.now(), ResumeOrigin.ADAPTED, ANALYSIS_ID.value(), false, 85, List.of())));

		RewriteResult result = service.latest(ANALYSIS_ID).orElseThrow();

		assertThat(result.resumeId()).isEqualTo(RESUME_ID);
		assertThat(result.docxResumeVersionId()).isEqualTo(DOCX_ID);
		assertThat(result.pdfResumeVersionId()).isEqualTo(PDF_ID);
		assertThat(result.content()).isEqualTo(content);
		assertThat(result.contact()).isEqualTo(contact);
		assertThat(result.template()).isEqualTo(ResumeTemplate.MODERN_BLUE);
		assertThat(result.scoreBefore()).isEqualTo(60);
		assertThat(result.scoreAfter()).isEqualTo(85);
		assertThat(result.jobHighlighted()).isTrue();
		assertThat(result.aiModel()).isEqualTo("gpt-4o-mini");
		assertThat(result.originalSections()).isEmpty();
	}

	@Test
	void findsNothingBeforeTheResumeIsAdapted() {
		when(analysisSnapshots.find(ANALYSIS_ID)).thenReturn(Optional.of(snapshot()));
		when(resumeService.latestAdaptedVersion(ANALYSIS_ID.value())).thenReturn(Optional.empty());

		assertThat(service.latest(ANALYSIS_ID)).isEmpty();
		verifyNoInteractions(contentStore, rewriteRepository);
	}

	private static AnalysisSnapshot snapshot() {
		return new AnalysisSnapshot(UUID.randomUUID(), AnalysisStatus.COMPLETED, 60, AnalysisMode.JOB_MATCH, "vaga", null);
	}
}
