package com.diegohaefliger.atsresumeoptimizer.coverletter.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsage;
import com.diegohaefliger.atsresumeoptimizer.ai.CoverLetterBrief;
import com.diegohaefliger.atsresumeoptimizer.ai.CoverLetterDraft;
import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisSnapshot;
import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisSnapshotPort;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisStatus;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetter;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetterRequiresAdaptedResumeException;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetterRequiresJobException;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionText;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CoverLetterServiceImplTest {

	private static final AnalysisId ANALYSIS_ID = new AnalysisId(UUID.randomUUID());
	private static final UUID JOB_ID = UUID.randomUUID();
	private static final UUID ADAPTED_VERSION_ID = UUID.randomUUID();
	private static final Instant NOW = Instant.parse("2026-10-07T12:00:00Z");

	@Mock
	private CoverLetterRepository repository;
	@Mock
	private AnalysisSnapshotPort analysisSnapshots;
	@Mock
	private JobStructuringService jobStructuringService;
	@Mock
	private ResumeService resumeService;
	@Mock
	private AiPort aiPort;

	private CoverLetterServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new CoverLetterServiceImpl(repository, new CoverLetterEntityMapperImpl(), analysisSnapshots,
				jobStructuringService, resumeService, aiPort, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void writesTheCoverLetterFromTheAdaptedResumeAndSavesItForTheJob() {
		jobMatch();
		when(resumeService.latestAdaptedVersion(ANALYSIS_ID.value())).thenReturn(Optional.of(ADAPTED_VERSION_ID));
		when(resumeService.getVersionText(ADAPTED_VERSION_ID)).thenReturn(new ResumeVersionText("currículo adaptado", null));
		when(jobStructuringService.offer(JOB_ID)).thenReturn(Optional.of(job()));
		when(jobStructuringService.selectedKeywords(JOB_ID)).thenReturn(List.of("Java", "Kafka"));
		when(repository.findByJobPostingId(JOB_ID)).thenReturn(Optional.empty());
		when(aiPort.writeCoverLetter(any())).thenReturn(new AiResult<>(new CoverLetterDraft("  Texto da apresentação.  "),
				new AiUsage("OpenAI", "gpt-4o-mini", 300, 120, new BigDecimal("0.0010"), false)));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		CoverLetter saved = service.generate(ANALYSIS_ID);

		ArgumentCaptor<CoverLetterBrief> brief = ArgumentCaptor.forClass(CoverLetterBrief.class);
		verify(aiPort).writeCoverLetter(brief.capture());
		assertThat(brief.getValue()).isEqualTo(new CoverLetterBrief("Dev Java", "Cielo", "texto da vaga",
				List.of("Java", "Kafka"), "currículo adaptado"));
		assertThat(saved.jobPostingId()).isEqualTo(JOB_ID);
		assertThat(saved.analysisId()).isEqualTo(ANALYSIS_ID.value());
		assertThat(saved.content()).isEqualTo("Texto da apresentação.");
		assertThat(saved.aiModel()).isEqualTo("gpt-4o-mini");
		assertThat(saved.updatedAt()).isEqualTo(NOW);
	}

	@Test
	void replacesTheCoverLetterAlreadySavedForTheJob() {
		jobMatch();
		CoverLetterEntity existing = new CoverLetterEntity(JOB_ID, Instant.parse("2026-10-01T00:00:00Z"));
		existing.setContent("antiga");
		when(repository.findByJobPostingId(JOB_ID)).thenReturn(Optional.of(existing));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		CoverLetter saved = service.save(ANALYSIS_ID, "Texto editado\n");

		assertThat(saved.id()).isEqualTo(existing.getId());
		assertThat(saved.content()).isEqualTo("Texto editado");
		assertThat(saved.updatedAt()).isEqualTo(NOW);
	}

	@Test
	void refusesToWriteBeforeTheResumeIsAdapted() {
		jobMatch();
		when(resumeService.latestAdaptedVersion(ANALYSIS_ID.value())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.generate(ANALYSIS_ID)).isInstanceOf(CoverLetterRequiresAdaptedResumeException.class);
		verifyNoInteractions(aiPort);
	}

	@Test
	void refusesAnAnalysisWithoutAJob() {
		when(analysisSnapshots.find(ANALYSIS_ID)).thenReturn(Optional.of(snapshot(AnalysisMode.GENERAL)));

		assertThatThrownBy(() -> service.generate(ANALYSIS_ID)).isInstanceOf(CoverLetterRequiresJobException.class);
		assertThat(service.find(ANALYSIS_ID)).isEmpty();
		verifyNoInteractions(aiPort, repository);
	}

	@Test
	void findsTheCoverLetterOfTheAnalysisJob() {
		jobMatch();
		CoverLetterEntity existing = new CoverLetterEntity(JOB_ID, NOW);
		existing.setContent("Texto");
		when(repository.findByJobPostingId(JOB_ID)).thenReturn(Optional.of(existing));

		assertThat(service.find(ANALYSIS_ID)).map(CoverLetter::content).contains("Texto");
	}

	private void jobMatch() {
		when(analysisSnapshots.find(ANALYSIS_ID)).thenReturn(Optional.of(snapshot(AnalysisMode.JOB_MATCH)));
		when(analysisSnapshots.jobPostingId(ANALYSIS_ID)).thenReturn(Optional.of(JOB_ID));
	}

	private static AnalysisSnapshot snapshot(AnalysisMode mode) {
		return new AnalysisSnapshot(UUID.randomUUID(), AnalysisStatus.COMPLETED, 80, mode, "vaga", null);
	}

	private static JobOffer job() {
		return new JobOffer(JOB_ID, 1L, "Dev Java", "Cielo", null, null, null, null, null, null, List.of(), null, null,
				"texto da vaga");
	}
}
