package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisRequested;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisStatus;
import com.diegohaefliger.atsresumeoptimizer.job.JobDetails;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringResult;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.parsing.application.BulletTextExtractor;
import com.diegohaefliger.atsresumeoptimizer.parsing.application.ResumeAnalysisPipeline;
import com.diegohaefliger.atsresumeoptimizer.parsing.application.SensitiveDataDetector;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ContactInfo;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.NormalizedDocument;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingSignals;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionCreated;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Dimension;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionResult;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringOutcome;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringProfileLookup;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringProfileProvider;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringProfileV1;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceImplTest {

	@Mock
	private ResumeAnalysisPipeline parsingPipeline;
	@Mock
	private ResumeService resumeService;
	@Mock
	private JobStructuringService jobStructuringService;
	@Mock
	private ScoringService scoringService;
	@Mock
	private ScoringProfileProvider scoringProfileProvider;
	@Mock
	private AiPort aiPort;
	@Mock
	private AnalysisRepository analysisRepository;
	@Mock
	private AnalysisResultRecorder resultRecorder;
	@Mock
	private AnalysisEventEmitterRegistry eventEmitterRegistry;
	@Mock
	private ApplicationEventPublisher eventPublisher;
	private final SensitiveDataDetector sensitiveDataDetector = new SensitiveDataDetector();

	private AnalysisServiceImpl service;

	@Test
	void createQueuesTheAnalysisAndPublishesAnEventInsteadOfRunningThePipelineInline() {
		service = newService();
		var upload = new ResumeUpload("curriculo.pdf", "application/pdf");
		byte[] content = "conteudo".getBytes();
		when(resumeService.upload(content, "curriculo.pdf", "application/pdf"))
				.thenReturn(new ResumeVersionCreated(UUID.randomUUID(), UUID.randomUUID(), "key", 8, "sha", 1));

		var details = new JobDetails("Acme", "https://acme.com/vaga", WorkModel.REMOTE, null, null, null, null, null, null);
		UUID jobPostingId = UUID.randomUUID();
		when(jobStructuringService.register("vaga de java", details)).thenReturn(jobPostingId);

		AnalysisCreation creation = service.create(upload, content, "vaga de java", null, details);

		assertThat(creation.status()).isEqualTo(AnalysisStatus.PENDING);
		ArgumentCaptor<AnalysisEntity> saved = ArgumentCaptor.forClass(AnalysisEntity.class);
		verify(analysisRepository).save(saved.capture());
		assertThat(saved.getValue().jobPostingId()).isEqualTo(jobPostingId);
		verify(eventPublisher).publishEvent(any(AnalysisRequested.class));
		verify(jobStructuringService, never()).structureFromText(any());
		verifyNoInteractions(parsingPipeline, scoringService, scoringProfileProvider, aiPort);
	}

	@Test
	void theListenerRunsTheFullPipelineForAJobMatchAnalysisAndPersistsTheResult() {
		service = newService();
		AnalysisEntity entity = new AnalysisEntity(UUID.randomUUID(), UUID.randomUUID(),
				AnalysisMode.JOB_MATCH, null, "vaga de java", Instant.now());
		when(analysisRepository.findById(entity.id())).thenReturn(Optional.of(entity));

		byte[] content = "conteudo".getBytes();
		when(resumeService.downloadContent(entity.resumeVersionId())).thenReturn(content);
		NormalizedDocument document = new NormalizedDocument(SourceFormat.PDF, "Ana Silva\nExperiencia", "", 1);
		ParsingResult parsingResult = new ParsingResult(document, new ParsingSignals(false, false, false, false, false,
				false, false, false), List.of(), new ContactInfo(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
		when(parsingPipeline.analyze(content)).thenReturn(parsingResult);

		JobStructured structured = new JobStructured("Backend Java", "pleno", 3, null, List.of(), List.of(), Map.of());
		when(jobStructuringService.structureFromText(any()))
				.thenReturn(new JobStructuringResult(UUID.randomUUID(), structured, null));

		when(scoringProfileProvider.activeProfile(AnalysisMode.JOB_MATCH))
				.thenReturn(new ScoringProfileLookup(UUID.randomUUID(), ScoringProfileV1.JOB_MATCH));
		when(scoringService.score(any(), any())).thenReturn(
				new ScoringOutcome(72, List.of(DimensionResult.of(Dimension.PARSEABILITY, 80, List.of())), List.of()));

		service.onAnalysisRequested(new AnalysisRequested(entity.id()));

		assertThat(entity.status()).isEqualTo(AnalysisStatus.COMPLETED);
		verify(resultRecorder).record(eq(entity.id()), any(), eq(ScoringProfileV1.JOB_MATCH));
		verify(eventEmitterRegistry).complete(entity.id());
		verifyNoInteractions(aiPort);
	}

	@Test
	void onlySendsRealBulletLinesToTheAiReviewJoiningLinesThatWrappedInThePdf() {
		service = newService();
		AnalysisEntity entity = new AnalysisEntity(UUID.randomUUID(), UUID.randomUUID(),
				AnalysisMode.GENERAL, null, null, Instant.now());
		when(analysisRepository.findById(entity.id())).thenReturn(Optional.of(entity));

		byte[] content = "conteudo".getBytes();
		when(resumeService.downloadContent(entity.resumeVersionId())).thenReturn(content);
		NormalizedDocument document = new NormalizedDocument(SourceFormat.PDF, "Ana Silva", "", 1);
		String experienceContent = String.join("\n",
				"Desenvolvedora Backend Sênior — Jan 2020 – Dez 2022",
				"Empresa X | São Paulo, SP",
				"• Desenvolvimento e sustentação de microsserviços back-end do domínio de cartões, com foco em",
				"estabilidade em produção.",
				"• Liderança técnica do squad.");
		Section experience = new Section("EXPERIENCIA", true, experienceContent);
		ParsingResult parsingResult = new ParsingResult(document, new ParsingSignals(false, false, false, false, false,
				false, false, false), List.of(experience),
				new ContactInfo(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
						Optional.empty()));
		when(parsingPipeline.analyze(content)).thenReturn(parsingResult);

		when(scoringProfileProvider.activeProfile(AnalysisMode.GENERAL))
				.thenReturn(new ScoringProfileLookup(UUID.randomUUID(), ScoringProfileV1.GENERAL));
		when(aiPort.reviewBullets(any())).thenReturn(new AiResult<>(List.of(), null));
		when(scoringService.score(any(), any())).thenReturn(
				new ScoringOutcome(72, List.of(DimensionResult.of(Dimension.PARSEABILITY, 80, List.of())), List.of()));

		service.onAnalysisRequested(new AnalysisRequested(entity.id()));

		@SuppressWarnings("unchecked")
		var bulletsCaptor = ArgumentCaptor.forClass(List.class);
		verify(aiPort).reviewBullets(bulletsCaptor.capture());
		assertThat(bulletsCaptor.getValue()).containsExactly(
				"• Desenvolvimento e sustentação de microsserviços back-end do domínio de cartões, com foco em estabilidade em produção.",
				"• Liderança técnica do squad.");
	}

	@Test
	void theListenerIsIdempotentWhenTheAnalysisIsNoLongerPending() {
		service = newService();
		AnalysisEntity entity = new AnalysisEntity(UUID.randomUUID(), UUID.randomUUID(),
				AnalysisMode.GENERAL, null, null, Instant.now());
		entity.startParsing();
		when(analysisRepository.findById(entity.id())).thenReturn(Optional.of(entity));

		service.onAnalysisRequested(new AnalysisRequested(entity.id()));

		verifyNoInteractions(resumeService, parsingPipeline, jobStructuringService, scoringService);
	}

	@Test
	void createFromExistingVersionValidatesTheResumeVersionResolvesTheJobTextAndQueuesTheAnalysis() {
		service = newService();
		UUID resumeId = UUID.randomUUID();
		UUID resumeVersionId = UUID.randomUUID();
		UUID jobPostingId = UUID.randomUUID();
		when(jobStructuringService.getRawText(jobPostingId)).thenReturn("vaga de java");

		AnalysisCreation creation = service.createFromExistingVersion(resumeId, resumeVersionId, jobPostingId, null, null, null);

		assertThat(creation.status()).isEqualTo(AnalysisStatus.PENDING);
		verify(resumeService).assertExists(resumeId, resumeVersionId);
		verify(analysisRepository).save(any());
		verify(eventPublisher).publishEvent(any(AnalysisRequested.class));
	}

	@Test
	void createFromExistingVersionSkipsJobLookupWhenNoJobIdIsGiven() {
		service = newService();
		UUID resumeId = UUID.randomUUID();
		UUID resumeVersionId = UUID.randomUUID();

		AnalysisCreation creation = service.createFromExistingVersion(resumeId, resumeVersionId, null, null, "Backend Java", null);

		assertThat(creation.status()).isEqualTo(AnalysisStatus.PENDING);
		verifyNoInteractions(jobStructuringService);
	}

	@Test
	void createFromExistingVersionRegistersAJobPastedNowAndLinksItToTheAnalysis() {
		service = newService();
		UUID resumeId = UUID.randomUUID();
		UUID resumeVersionId = UUID.randomUUID();
		UUID registeredJobId = UUID.randomUUID();
		JobDetails details = new JobDetails("Acme", null, WorkModel.REMOTE, null, null, null, null, null, null);
		when(jobStructuringService.register("vaga de java", details)).thenReturn(registeredJobId);

		service.createFromExistingVersion(resumeId, resumeVersionId, null, "vaga de java", null, details);

		ArgumentCaptor<AnalysisEntity> captor = ArgumentCaptor.forClass(AnalysisEntity.class);
		verify(analysisRepository).save(captor.capture());
		assertThat(captor.getValue().jobPostingId()).isEqualTo(registeredJobId);
		assertThat(captor.getValue().mode()).isEqualTo(AnalysisMode.JOB_MATCH);
		verify(jobStructuringService, never()).getRawText(any());
	}

	private AnalysisServiceImpl newService() {
		return new AnalysisServiceImpl(parsingPipeline, resumeService, jobStructuringService, scoringService,
				scoringProfileProvider, aiPort, analysisRepository, resultRecorder, eventEmitterRegistry, eventPublisher, sensitiveDataDetector,
				new BulletTextExtractor());
	}
}
