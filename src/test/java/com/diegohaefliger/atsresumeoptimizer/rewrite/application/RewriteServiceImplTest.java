package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsage;
import com.diegohaefliger.atsresumeoptimizer.ai.JobFocus;
import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine;
import com.diegohaefliger.atsresumeoptimizer.ai.RemovedSkill;
import com.diegohaefliger.atsresumeoptimizer.ai.RequirementEvidence;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisSnapshot;
import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisSnapshotPort;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisStatus;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringResult;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
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
import com.diegohaefliger.atsresumeoptimizer.rewrite.RemovedSkillView;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewriteResult;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.AnalysisNotReadyForRewriteException;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.JobHighlight;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.RewriteFailedException;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.RewriteRateLimitExceededException;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Dimension;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionResult;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Finding;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.FindingCode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.JobKeywordMatcher;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.KeywordMatch;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringOutcome;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringProfile;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringProfileLookup;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringProfileProvider;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
class RewriteServiceImplTest {

	@Mock
	private AnalysisSnapshotPort analysisSnapshotPort;
	@Mock
	private ResumeContentStore contentStore;
	@Mock
	private ResumeAnalysisPipeline parsingPipeline;
	@Mock
	private ResumeService resumeService;
	@Mock
	private AiPort aiPort;
	@Mock
	private RewriteRateLimiter rateLimiter;
	@Mock
	private RewriteRepository rewriteRepository;
	@Mock
	private DocxTemplateRenderer docxRenderer;
	@Mock
	private PdfTemplateRenderer pdfRenderer;
	@Mock
	private ScoringService scoringService;
	@Mock
	private JobKeywordMatcher jobKeywordMatcher;
	@Mock
	private ScoringProfileProvider scoringProfileProvider;
	@Mock
	private JobStructuringService jobStructuringService;
	private final SensitiveDataDetector sensitiveDataDetector = new SensitiveDataDetector();

	@Test
	void refusesToRewriteAnAnalysisThatHasNotFinished() {
		RewriteServiceImpl service = newService();
		AnalysisId analysisId = AnalysisId.generate();
		when(analysisSnapshotPort.find(analysisId)).thenReturn(Optional.of(
				new AnalysisSnapshot(UUID.randomUUID(), AnalysisStatus.ANALYZING, null, AnalysisMode.GENERAL, null, null)));

		assertThatThrownBy(() -> service.rewrite(analysisId, ResumeTemplate.CLASSIC, JobHighlight.AUTO))
				.isInstanceOf(AnalysisNotReadyForRewriteException.class);
		verifyNoInteractions(resumeService, aiPort, rewriteRepository, docxRenderer, pdfRenderer);
	}

	@Test
	void refusesWhenTheRateLimitWasAlreadyConsumed() {
		RewriteServiceImpl service = newService();
		AnalysisId analysisId = AnalysisId.generate();
		when(analysisSnapshotPort.find(analysisId)).thenReturn(Optional.of(
				new AnalysisSnapshot(UUID.randomUUID(), AnalysisStatus.COMPLETED, 60, AnalysisMode.GENERAL, null, null)));
		when(rateLimiter.tryConsume()).thenReturn(false);

		assertThatThrownBy(() -> service.rewrite(analysisId, ResumeTemplate.CLASSIC, JobHighlight.AUTO))
				.isInstanceOf(RewriteRateLimitExceededException.class);
		verifyNoInteractions(resumeService, aiPort, rewriteRepository, docxRenderer, pdfRenderer);
	}

	@Test
	void rewritesBulletsAndStoresOnlyTheDocxAsANewAdaptedResumeLinkedToTheAnalysis() {
		RewriteServiceImpl service = newService();
		AnalysisId analysisId = AnalysisId.generate();
		UUID sourceVersionId = UUID.randomUUID();
		UUID resumeId = UUID.randomUUID();
		when(analysisSnapshotPort.find(analysisId)).thenReturn(Optional.of(
				new AnalysisSnapshot(sourceVersionId, AnalysisStatus.COMPLETED, 60, AnalysisMode.GENERAL, null, null)));
		when(rateLimiter.tryConsume()).thenReturn(true);
		when(resumeService.downloadContent(sourceVersionId)).thenReturn("bytes".getBytes());

		Section section = new Section("EXPERIENCIA", true, "• Responsavel por testes manuais\n");
		ParsingResult parsingResult = new ParsingResult(
				new NormalizedDocument(SourceFormat.PDF, "raw", "raw", 1),
				new ParsingSignals(false, false, false, false, false, false, false, false),
				List.of(section),
				new ContactInfo(Optional.of("ana@email.com"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
						Optional.empty()));
		when(parsingPipeline.analyze(any())).thenReturn(parsingResult);

		ScoringProfile scoringProfile = new ScoringProfile("v1", 1, AnalysisMode.GENERAL, Map.of(Dimension.CONTENT_QUALITY, 1.0));
		when(scoringProfileProvider.activeProfile(AnalysisMode.GENERAL))
				.thenReturn(new ScoringProfileLookup(UUID.randomUUID(), scoringProfile));
		when(scoringService.score(eq(scoringProfile), any())).thenReturn(new ScoringOutcome(85, List.of(), List.of()));

		ResumeEntry entry = new ResumeEntry("Responsavel por testes manuais", null, null, null,
				List.of(List.of(new TextSpan("Reduzi bugs em 40% através de testes automatizados", false))), null);
		ResumeSection experienceSection = new ResumeSection("EXPERIENCIA", ResumeSectionSemanticType.EXPERIENCE,
				ResumeSectionKind.ENTRIES, null, null, null, List.of(entry));
		when(aiPort.structureResume(any(), any(), any())).thenReturn(new AiResult<>(
				new StructuredResume("Ana", null, List.of(experienceSection)),
				new AiUsage("gpt-4o-mini", 50, 25, BigDecimal.valueOf(0.002), false)));

		when(docxRenderer.render(eq(ResumeTheme.of(ResumeTemplate.CLASSIC)), any(), any()))
				.thenReturn("docx".getBytes());
		when(pdfRenderer.render(eq(ResumeTheme.of(ResumeTemplate.CLASSIC)), any(), any()))
				.thenReturn("pdf".getBytes());

		UUID docxVersionId = UUID.randomUUID();
		when(resumeService.storeAdapted(eq(sourceVersionId), any(), anyString(), any(), anyString(), anyString(),
				anyString()))
				.thenReturn(new ResumeVersionCreated(resumeId, docxVersionId, "key-docx", 4, "sha", null));

		RewriteResult result = service.rewrite(analysisId, ResumeTemplate.CLASSIC, JobHighlight.AUTO);

		assertThat(result.resumeId()).isEqualTo(resumeId);
		assertThat(result.docxResumeVersionId()).isEqualTo(docxVersionId);
		assertThat(result.pdfResumeVersionId()).isNull();
		assertThat(result.bullets()).hasSize(1);
		assertThat(result.bullets().getFirst().needsConfirmation()).isTrue();
		assertThat(result.scoreBefore()).isEqualTo(60);
		assertThat(result.scoreAfter()).isEqualTo(85);
		verify(resumeService).updateAtsScore(result.resumeId(), 85);
		assertThat(result.jobHighlighted()).isFalse();
		verify(rewriteRepository).save(any());
		verify(resumeService).storeAdapted(eq(sourceVersionId), eq(analysisId.value()), eq("adaptado na avaliação geral"),
				any(), anyString(), anyString(), anyString());
		verify(resumeService, never()).storeGeneratedVersion(any(), any(), anyString(), anyString(), anyString());
	}

	@Test
	void feedsFindingsFromTheOriginalContentAsCorrectionInstructionsToTheStructuringCall() {
		RewriteServiceImpl service = newService();
		AnalysisId analysisId = AnalysisId.generate();
		UUID sourceVersionId = UUID.randomUUID();
		UUID resumeId = UUID.randomUUID();
		when(analysisSnapshotPort.find(analysisId)).thenReturn(Optional.of(
				new AnalysisSnapshot(sourceVersionId, AnalysisStatus.COMPLETED, 60, AnalysisMode.GENERAL, null, null)));
		when(rateLimiter.tryConsume()).thenReturn(true);
		when(resumeService.downloadContent(sourceVersionId)).thenReturn("bytes".getBytes());

		Section section = new Section("EXPERIENCIA", true, "• Responsavel por testes manuais\n");
		ParsingResult parsingResult = new ParsingResult(
				new NormalizedDocument(SourceFormat.PDF, "raw", "raw", 1),
				new ParsingSignals(false, false, false, false, false, false, false, false),
				List.of(section),
				new ContactInfo(Optional.of("ana@email.com"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
						Optional.empty()));
		when(parsingPipeline.analyze(any())).thenReturn(parsingResult);

		ScoringProfile scoringProfile = new ScoringProfile("v1", 1, AnalysisMode.GENERAL, Map.of(Dimension.CONTENT_QUALITY, 1.0));
		when(scoringProfileProvider.activeProfile(AnalysisMode.GENERAL))
				.thenReturn(new ScoringProfileLookup(UUID.randomUUID(), scoringProfile));

		Finding weakVerbFinding = Finding.of(FindingCode.WEAK_ACTION_VERB,
				"1 linha(s) começam com verbo fraco (\"auxiliei\", \"responsável por\"...).",
				"Comece cada bullet com um verbo de ação forte no passado (\"Liderei\", \"Implementei\", \"Reduzi\").");
		ScoringOutcome originalOutcome = new ScoringOutcome(60,
				List.of(DimensionResult.of(Dimension.CONTENT_QUALITY, 70, List.of(weakVerbFinding))), List.of());
		ScoringOutcome rewrittenOutcome = new ScoringOutcome(85, List.of(), List.of());
		when(scoringService.score(eq(scoringProfile), any())).thenReturn(originalOutcome, rewrittenOutcome);

		when(aiPort.structureResume(any(), any(), any())).thenReturn(new AiResult<>(
				new StructuredResume("Ana", null, List.of(experienceWith("Implementei testes manuais"))),
				new AiUsage("gpt-4o-mini", 50, 25, BigDecimal.valueOf(0.002), false)));

		when(docxRenderer.render(eq(ResumeTheme.of(ResumeTemplate.CLASSIC)), any(), any()))
				.thenReturn("docx".getBytes());
		when(pdfRenderer.render(eq(ResumeTheme.of(ResumeTemplate.CLASSIC)), any(), any()))
				.thenReturn("pdf".getBytes());
		when(resumeService.storeAdapted(eq(sourceVersionId), any(), anyString(), any(), anyString(), anyString(),
				anyString()))
				.thenReturn(new ResumeVersionCreated(resumeId, UUID.randomUUID(), "key-docx", 4, "sha", null));

		service.rewrite(analysisId, ResumeTemplate.CLASSIC, JobHighlight.AUTO);

		var instructionsCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
		verify(aiPort).structureResume(any(), instructionsCaptor.capture(), any());
		assertThat(instructionsCaptor.getValue()).contains("verbo fraco");
	}

	@Test
	void leavesTheBulletPreviewEmptyWhenNoBulletMarkerWasFoundInTheOriginal() {
		RewriteServiceImpl service = newService();
		AnalysisId analysisId = AnalysisId.generate();
		UUID sourceVersionId = UUID.randomUUID();
		UUID resumeId = UUID.randomUUID();
		when(analysisSnapshotPort.find(analysisId)).thenReturn(Optional.of(
				new AnalysisSnapshot(sourceVersionId, AnalysisStatus.COMPLETED, 60, AnalysisMode.GENERAL, null, null)));
		when(rateLimiter.tryConsume()).thenReturn(true);
		when(resumeService.downloadContent(sourceVersionId)).thenReturn("bytes".getBytes());

		// Seção sem nenhuma linha com marcador de bullet — extractBullets fica vazio.
		Section section = new Section("EXPERIENCIA", true, "Cargo sem bullet nenhum, só um parágrafo corrido.");
		ParsingResult parsingResult = new ParsingResult(
				new NormalizedDocument(SourceFormat.PDF, "raw", "raw", 1),
				new ParsingSignals(false, false, false, false, false, false, false, false),
				List.of(section),
				new ContactInfo(Optional.of("ana@email.com"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
						Optional.empty()));
		when(parsingPipeline.analyze(any())).thenReturn(parsingResult);

		ScoringProfile scoringProfile = new ScoringProfile("v1", 1, AnalysisMode.GENERAL, Map.of(Dimension.CONTENT_QUALITY, 1.0));
		when(scoringProfileProvider.activeProfile(AnalysisMode.GENERAL))
				.thenReturn(new ScoringProfileLookup(UUID.randomUUID(), scoringProfile));
		when(scoringService.score(eq(scoringProfile), any())).thenReturn(new ScoringOutcome(85, List.of(), List.of()));

		when(aiPort.structureResume(any(), any(), any())).thenReturn(new AiResult<>(
				new StructuredResume("Ana", null, List.of()),
				new AiUsage("gpt-4o-mini", 50, 25, BigDecimal.valueOf(0.002), false)));

		when(docxRenderer.render(eq(ResumeTheme.of(ResumeTemplate.CLASSIC)), any(), any()))
				.thenReturn("docx".getBytes());
		when(pdfRenderer.render(eq(ResumeTheme.of(ResumeTemplate.CLASSIC)), any(), any()))
				.thenReturn("pdf".getBytes());
		when(resumeService.storeAdapted(eq(sourceVersionId), any(), anyString(), any(), anyString(), anyString(),
				anyString()))
				.thenReturn(new ResumeVersionCreated(resumeId, UUID.randomUUID(), "key-docx", 4, "sha", null));

		RewriteResult result = service.rewrite(analysisId, ResumeTemplate.CLASSIC, JobHighlight.AUTO);

		assertThat(result.bullets()).isEmpty();
		verify(rewriteRepository).save(any());
	}

	@Test
	void resolvesTheJobOnceForBothTheCorrectionInstructionsAndTheFinalScoring() {
		RewriteServiceImpl service = newService();
		AnalysisId analysisId = AnalysisId.generate();
		UUID sourceVersionId = UUID.randomUUID();
		UUID resumeId = UUID.randomUUID();
		when(analysisSnapshotPort.find(analysisId)).thenReturn(Optional.of(
				new AnalysisSnapshot(sourceVersionId, AnalysisStatus.COMPLETED, 60, AnalysisMode.JOB_MATCH, "Vaga de Java", null)));
		when(rateLimiter.tryConsume()).thenReturn(true);
		when(resumeService.downloadContent(sourceVersionId)).thenReturn("bytes".getBytes());

		// Sem marcador de bullet — extractBullets fica vazio, preview de bullet fica vazio também.
		Section section = new Section("EXPERIENCIA", true, "Responsavel por testes manuais, sem marcador de bullet.");
		ParsingResult parsingResult = new ParsingResult(
				new NormalizedDocument(SourceFormat.PDF, "raw", "raw", 1),
				new ParsingSignals(false, false, false, false, false, false, false, false),
				List.of(section),
				new ContactInfo(Optional.of("ana@email.com"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
						Optional.empty()));
		when(parsingPipeline.analyze(any())).thenReturn(parsingResult);

		JobStructured job = new JobStructured("Backend Java", "pleno", 3, null, List.of(), List.of("Kubernetes"), Map.of());
		when(jobStructuringService.structureFromText("Vaga de Java"))
				.thenReturn(new JobStructuringResult(UUID.randomUUID(), job, null));

		ScoringProfile scoringProfile = new ScoringProfile("v1", 1, AnalysisMode.JOB_MATCH, Map.of(Dimension.KEYWORD_MATCH, 1.0));
		when(scoringProfileProvider.activeProfile(AnalysisMode.JOB_MATCH))
				.thenReturn(new ScoringProfileLookup(UUID.randomUUID(), scoringProfile));

		// Achado de KEYWORD_MATCH existe (a análise original detecta normalmente), mas NÃO deve virar
		// correctionInstructions — ver o porquê no javadoc de correctionInstructions().
		Finding missingKeyword = Finding.of(FindingCode.KEYWORD_REQUIRED_MISSING,
				"A palavra-chave \"Kubernetes\", exigida pela vaga, não aparece no currículo.",
				"Inclua \"Kubernetes\" no currículo, se você realmente tiver essa experiência.");
		ScoringOutcome originalOutcome = new ScoringOutcome(60,
				List.of(DimensionResult.of(Dimension.KEYWORD_MATCH, 0, List.of(missingKeyword))), List.of());
		ScoringOutcome rewrittenOutcome = new ScoringOutcome(85, List.of(), List.of());
		when(scoringService.score(eq(scoringProfile), any())).thenReturn(originalOutcome, rewrittenOutcome);

		when(aiPort.structureResume(any(), any(), any())).thenReturn(new AiResult<>(
				new StructuredResume("Ana", null, List.of()),
				new AiUsage("gpt-4o-mini", 50, 25, BigDecimal.valueOf(0.002), false)));

		when(docxRenderer.render(eq(ResumeTheme.of(ResumeTemplate.CLASSIC)), any(), any()))
				.thenReturn("docx".getBytes());
		when(pdfRenderer.render(eq(ResumeTheme.of(ResumeTemplate.CLASSIC)), any(), any()))
				.thenReturn("pdf".getBytes());
		when(resumeService.storeAdapted(eq(sourceVersionId), any(), anyString(), any(), anyString(), anyString(),
				anyString()))
				.thenReturn(new ResumeVersionCreated(resumeId, UUID.randomUUID(), "key-docx", 4, "sha", null));

		RewriteResult result = service.rewrite(analysisId, ResumeTemplate.CLASSIC, JobHighlight.AUTO);

		verify(jobStructuringService, org.mockito.Mockito.times(1)).structureFromText("Vaga de Java");
		var instructionsCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
		verify(aiPort).structureResume(any(), instructionsCaptor.capture(), any());
		assertThat(instructionsCaptor.getValue()).doesNotContain("Kubernetes");
		assertThat(result.scoreAfter()).isEqualTo(85);
	}

	@Test
	void overridesTheSectionTitleWithTheCanonicalOneButLeavesOtherSectionsAsTheAiWroteThem() {
		RewriteServiceImpl service = newService();
		AnalysisId analysisId = AnalysisId.generate();
		UUID sourceVersionId = UUID.randomUUID();
		UUID resumeId = UUID.randomUUID();
		when(analysisSnapshotPort.find(analysisId)).thenReturn(Optional.of(
				new AnalysisSnapshot(sourceVersionId, AnalysisStatus.COMPLETED, 60, AnalysisMode.GENERAL, null, null)));
		when(rateLimiter.tryConsume()).thenReturn(true);
		when(resumeService.downloadContent(sourceVersionId)).thenReturn("bytes".getBytes());

		Section section = new Section("EXPERIENCIA", true, "Parágrafo sem bullet.");
		ParsingResult parsingResult = new ParsingResult(
				new NormalizedDocument(SourceFormat.PDF, "raw", "raw", 1),
				new ParsingSignals(false, false, false, false, false, false, false, false),
				List.of(section),
				new ContactInfo(Optional.of("ana@email.com"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
						Optional.empty()));
		when(parsingPipeline.analyze(any())).thenReturn(parsingResult);

		ScoringProfile scoringProfile = new ScoringProfile("v1", 1, AnalysisMode.GENERAL, Map.of(Dimension.CONTENT_QUALITY, 1.0));
		when(scoringProfileProvider.activeProfile(AnalysisMode.GENERAL))
				.thenReturn(new ScoringProfileLookup(UUID.randomUUID(), scoringProfile));
		when(scoringService.score(eq(scoringProfile), any())).thenReturn(new ScoringOutcome(85, List.of(), List.of()));

		ResumeSection nonStandardProjects = new ResumeSection(
				"PROJETOS PESSOAIS", ResumeSectionSemanticType.PROJECTS, ResumeSectionKind.PARAGRAPH, "Bot de trading.",
				null, null, null);
		ResumeSection otherSection = new ResumeSection(
				"Hobbies Curiosos", ResumeSectionSemanticType.OTHER, ResumeSectionKind.PARAGRAPH, "Xadrez.", null, null, null);
		when(aiPort.structureResume(any(), any(), any())).thenReturn(new AiResult<>(
				new StructuredResume("Ana", null, List.of(nonStandardProjects, otherSection)),
				new AiUsage("gpt-4o-mini", 50, 25, BigDecimal.valueOf(0.002), false)));

		var contentCaptor = org.mockito.ArgumentCaptor.forClass(StructuredResume.class);
		when(docxRenderer.render(eq(ResumeTheme.of(ResumeTemplate.CLASSIC)), contentCaptor.capture(), any()))
				.thenReturn("docx".getBytes());
		when(pdfRenderer.render(eq(ResumeTheme.of(ResumeTemplate.CLASSIC)), any(), any()))
				.thenReturn("pdf".getBytes());
		when(resumeService.storeAdapted(eq(sourceVersionId), any(), anyString(), any(), anyString(), anyString(),
				anyString()))
				.thenReturn(new ResumeVersionCreated(resumeId, UUID.randomUUID(), "key-docx", 4, "sha", null));

		service.rewrite(analysisId, ResumeTemplate.CLASSIC, JobHighlight.AUTO);

		List<ResumeSection> renderedSections = contentCaptor.getValue().sections();
		assertThat(renderedSections).hasSize(2);
		assertThat(renderedSections).filteredOn(s -> s.semanticType() == ResumeSectionSemanticType.PROJECTS)
				.extracting(ResumeSection::title).containsExactly("Projetos");
		assertThat(renderedSections).filteredOn(s -> s.semanticType() == ResumeSectionSemanticType.OTHER)
				.extracting(ResumeSection::title).containsExactly("Hobbies Curiosos");
	}

	@Test
	void sanitizesDoubleSpacesAndHyphenBeforeStateCodeInEveryTextFieldBeforeRendering() {
		RewriteServiceImpl service = newService();
		AnalysisId analysisId = AnalysisId.generate();
		UUID sourceVersionId = UUID.randomUUID();
		UUID resumeId = UUID.randomUUID();
		when(analysisSnapshotPort.find(analysisId)).thenReturn(Optional.of(
				new AnalysisSnapshot(sourceVersionId, AnalysisStatus.COMPLETED, 60, AnalysisMode.GENERAL, null, null)));
		when(rateLimiter.tryConsume()).thenReturn(true);
		when(resumeService.downloadContent(sourceVersionId)).thenReturn("bytes".getBytes());

		Section section = new Section("EXPERIENCIA", true, "Parágrafo sem bullet.");
		ParsingResult parsingResult = new ParsingResult(
				new NormalizedDocument(SourceFormat.PDF, "Ana Silva\nEXPERIENCIA\nParágrafo sem bullet.", "raw", 1),
				new ParsingSignals(false, false, false, false, false, false, false, false),
				List.of(section),
				new ContactInfo(Optional.of("ana@email.com"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
						Optional.empty()));
		when(parsingPipeline.analyze(any())).thenReturn(parsingResult);

		ScoringProfile scoringProfile = new ScoringProfile("v1", 1, AnalysisMode.GENERAL, Map.of(Dimension.CONTENT_QUALITY, 1.0));
		when(scoringProfileProvider.activeProfile(AnalysisMode.GENERAL))
				.thenReturn(new ScoringProfileLookup(UUID.randomUUID(), scoringProfile));
		when(scoringService.score(eq(scoringProfile), any())).thenReturn(new ScoringOutcome(85, List.of(), List.of()));

		ResumeEntry entryWithHyphenAndDoubleSpace = new ResumeEntry("Desenvolvedora  Backend", "Jan 2020 - Dez 2022",
				"Empresa X | Panambi-RS", null,
				List.of(List.of(new TextSpan("Reduzi  bugs em 40%", false))), null);
		ResumeSection experience = new ResumeSection("EXPERIENCIA", ResumeSectionSemanticType.EXPERIENCE,
				ResumeSectionKind.ENTRIES, null, null, null, List.of(entryWithHyphenAndDoubleSpace));
		when(aiPort.structureResume(any(), any(), any())).thenReturn(new AiResult<>(
				new StructuredResume("Ana  Silva", null, List.of(experience)),
				new AiUsage("gpt-4o-mini", 50, 25, BigDecimal.valueOf(0.002), false)));

		var contentCaptor = org.mockito.ArgumentCaptor.forClass(StructuredResume.class);
		when(docxRenderer.render(eq(ResumeTheme.of(ResumeTemplate.CLASSIC)), contentCaptor.capture(), any()))
				.thenReturn("docx".getBytes());
		when(pdfRenderer.render(eq(ResumeTheme.of(ResumeTemplate.CLASSIC)), any(), any()))
				.thenReturn("pdf".getBytes());
		when(resumeService.storeAdapted(eq(sourceVersionId), any(), anyString(), any(), anyString(), anyString(),
				anyString()))
				.thenReturn(new ResumeVersionCreated(resumeId, UUID.randomUUID(), "key-docx", 4, "sha", null));

		service.rewrite(analysisId, ResumeTemplate.CLASSIC, JobHighlight.AUTO);

		StructuredResume rendered = contentCaptor.getValue();
		assertThat(rendered.name()).isEqualTo("Ana Silva");
		ResumeEntry renderedEntry = rendered.sections().get(0).entries().get(0);
		assertThat(renderedEntry.heading()).isEqualTo("Desenvolvedora Backend");
		assertThat(renderedEntry.subheading()).isEqualTo("Empresa X | Panambi–RS");
		assertThat(renderedEntry.bullets().get(0).get(0).text()).isEqualTo("Reduzi bugs em 40%");
	}

	@Test
	void sendsOnlyTheJobKeywordsTheResumeAlreadyHasAsJobFocus() {
		RewriteServiceImpl service = newService();
		AnalysisId analysisId = AnalysisId.generate();
		ResumeSection skills = new ResumeSection("COMPETÊNCIAS", ResumeSectionSemanticType.SKILLS, ResumeSectionKind.KEY_VALUE,
				null, List.of(new KeyValueLine("Competências", "Java, Excel, Git, SQL")), null, null);
		StructuredResume aiOutput = new StructuredResume("Ana", null, List.of(skills),
				List.of(new RemovedSkill("Preparar café", "Sem relação com desenvolvimento")));
		ParsingResult parsingResult = stubJobMatchRewrite(analysisId, aiOutput,
				"EXPERIENCIA\nDesenvolvimento em Java.\nCOMPETÊNCIAS\nJava, Excel, Git, SQL, Preparar café",
				List.of(new Section("EXPERIENCIA", true, "Desenvolvimento em Java."),
						new Section("COMPETÊNCIAS", true, "Java, Excel, Git, SQL, Preparar café")));
		when(jobKeywordMatcher.match(eq(parsingResult), any())).thenReturn(List.of(
				new KeywordMatch("Java", null, true, true, false, 2, List.of()),
				new KeywordMatch("Kubernetes", null, true, false, false, 0, List.of()),
				new KeywordMatch("versionamento de código", null, true, false, false, 0, List.of())));
		when(aiPort.findRequirementEvidence(any(), eq(List.of("Kubernetes", "versionamento de código"))))
				.thenReturn(new AiResult<>(List.of(
						new RequirementEvidence("versionamento de código", "Git"),
						new RequirementEvidence("Kubernetes", "Docker Swarm")),
						new AiUsage("gpt-4o-mini", 10, 5, BigDecimal.valueOf(0.001), false)));

		RewriteResult result = service.rewrite(analysisId, ResumeTemplate.CLASSIC, JobHighlight.AUTO);

		var focusCaptor = org.mockito.ArgumentCaptor.forClass(JobFocus.class);
		verify(aiPort).structureResume(any(), any(), focusCaptor.capture());
		assertThat(focusCaptor.getValue()).isEqualTo(new JobFocus("Backend Java", "pleno", List.of("Java"),
				List.of(new RequirementEvidence("versionamento de código", "Git")),
				List.of(new com.diegohaefliger.atsresumeoptimizer.ai.PrioritySkill("Java", null, false))));
		assertThat(result.evidencedRequirements()).containsExactly(new RequirementEvidence("versionamento de código", "Git"));
		assertThat(result.jobHighlighted()).isTrue();
		assertThat(result.removedSkills())
				.containsExactly(new RemovedSkillView("Preparar café", "Sem relação com desenvolvimento"));
	}

	@Test
	void skipsTheJobFocusWhenTheUserTurnsItOff() {
		RewriteServiceImpl service = newService();
		AnalysisId analysisId = AnalysisId.generate();
		stubJobMatchRewrite(analysisId, new StructuredResume("Ana", null, List.of()));

		RewriteResult result = service.rewrite(analysisId, ResumeTemplate.CLASSIC, JobHighlight.DISABLED);

		verify(aiPort).structureResume(any(), any(), eq(JobFocus.NONE));
		verifyNoInteractions(jobKeywordMatcher);
		assertThat(result.jobHighlighted()).isFalse();
	}

	@Test
	void sendsTheHeaderToTheAiAndReplacesANameThatIsNotInTheOriginal() {
		RewriteServiceImpl service = newService();
		AnalysisId analysisId = AnalysisId.generate();
		ResumeSection emptyEducation = new ResumeSection("FORMAÇÃO ACADÊMICA", ResumeSectionSemanticType.EDUCATION,
				ResumeSectionKind.RICH_LINES, null, null, List.of(), null);
		ResumeSection summary = new ResumeSection("RESUMO", ResumeSectionSemanticType.SUMMARY, ResumeSectionKind.PARAGRAPH,
				"Desenvolvedora backend.", null, null, null);
		stubJobMatchRewrite(analysisId, new StructuredResume("nome completo da pessoa", null, List.of(summary, emptyEducation)),
				"Maria Exemplo\nDesenvolvedora Backend\nEXPERIENCIA\nDesenvolvimento em Java.");

		service.rewrite(analysisId, ResumeTemplate.CLASSIC, JobHighlight.DISABLED);

		var textCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
		verify(aiPort).structureResume(textCaptor.capture(), any(), any());
		assertThat(textCaptor.getValue()).startsWith("Maria Exemplo\nDesenvolvedora Backend\nEXPERIENCIA");
		var contentCaptor = org.mockito.ArgumentCaptor.forClass(StructuredResume.class);
		verify(docxRenderer).render(any(), contentCaptor.capture(), any());
		assertThat(contentCaptor.getValue().name()).isEqualTo("Maria Exemplo");
		assertThat(contentCaptor.getValue().sections()).extracting(ResumeSection::semanticType)
				.containsExactly(ResumeSectionSemanticType.SUMMARY);
	}

	@Test
	void asksTheAiAgainWhenTheFirstAnswerDroppedAJob() {
		RewriteServiceImpl service = newService();
		AnalysisId analysisId = AnalysisId.generate();
		StructuredResume complete = new StructuredResume("Ana", null, List.of(experienceWith("Desenvolvimento em Java.")));
		stubJobMatchRewrite(analysisId, complete, "raw", ONE_BULLET_EXPERIENCE);
		AiUsage usage = new AiUsage("gpt-4o-mini", 50, 25, BigDecimal.valueOf(0.002), false);
		when(aiPort.structureResume(any(), any(), any()))
				.thenReturn(new AiResult<>(new StructuredResume("Ana", null, List.of()), usage))
				.thenReturn(new AiResult<>(complete, usage));

		service.rewrite(analysisId, ResumeTemplate.CLASSIC, JobHighlight.ENABLED);

		ArgumentCaptor<String> instructionsCaptor = ArgumentCaptor.forClass(String.class);
		verify(aiPort, times(2)).structureResume(any(), instructionsCaptor.capture(), any());
		assertThat(instructionsCaptor.getAllValues().get(1)).contains("a IA descartou 1 de 1");
	}

	@Test
	@MockitoSettings(strictness = Strictness.LENIENT)
	void failsWhenTheRetryStillDropsContent() {
		RewriteServiceImpl service = newService();
		AnalysisId analysisId = AnalysisId.generate();
		stubJobMatchRewrite(analysisId, new StructuredResume("Ana", null, List.of()), "raw", ONE_BULLET_EXPERIENCE);

		assertThatThrownBy(() -> service.rewrite(analysisId, ResumeTemplate.CLASSIC, JobHighlight.ENABLED))
				.isInstanceOf(RewriteFailedException.class)
				.hasMessageContaining("a IA descartou 1 de 1");
		verify(aiPort, times(2)).structureResume(any(), any(), any());
	}

	private static final List<Section> ONE_BULLET_EXPERIENCE =
			List.of(new Section("EXPERIENCIA", true, "Cargo\n• Desenvolvimento em Java."));

	private ParsingResult stubJobMatchRewrite(AnalysisId analysisId, StructuredResume aiOutput) {
		return stubJobMatchRewrite(analysisId, aiOutput, "raw");
	}

	private ParsingResult stubJobMatchRewrite(AnalysisId analysisId, StructuredResume aiOutput, String rawText) {
		return stubJobMatchRewrite(analysisId, aiOutput, rawText,
				List.of(new Section("EXPERIENCIA", true, "Desenvolvimento em Java.")));
	}

	private ResumeSection experienceWith(String bullet) {
		ResumeEntry entry = new ResumeEntry("Cargo", null, null, null, List.of(List.of(new TextSpan(bullet, false))), null);
		return new ResumeSection("EXPERIENCIA", ResumeSectionSemanticType.EXPERIENCE, ResumeSectionKind.ENTRIES, null, null,
				null, List.of(entry));
	}

	private ParsingResult stubJobMatchRewrite(AnalysisId analysisId, StructuredResume aiOutput, String rawText,
			List<Section> sections) {
		UUID sourceVersionId = UUID.randomUUID();
		UUID resumeId = UUID.randomUUID();
		when(analysisSnapshotPort.find(analysisId)).thenReturn(Optional.of(
				new AnalysisSnapshot(sourceVersionId, AnalysisStatus.COMPLETED, 60, AnalysisMode.JOB_MATCH, "Vaga de Java", null)));
		when(rateLimiter.tryConsume()).thenReturn(true);
		when(resumeService.downloadContent(sourceVersionId)).thenReturn("bytes".getBytes());
		ParsingResult parsingResult = new ParsingResult(
				new NormalizedDocument(SourceFormat.PDF, rawText, rawText, 1),
				new ParsingSignals(false, false, false, false, false, false, false, false),
				sections,
				new ContactInfo(Optional.of("ana@email.com"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
						Optional.empty()));
		when(parsingPipeline.analyze(any())).thenReturn(parsingResult);
		JobStructured job = new JobStructured("Backend Java", "pleno", 3, null, List.of(), List.of("Java", "Kubernetes"), Map.of());
		when(jobStructuringService.structureFromText("Vaga de Java")).thenReturn(new JobStructuringResult(UUID.randomUUID(), job, null));
		ScoringProfile scoringProfile = new ScoringProfile("v1", 1, AnalysisMode.JOB_MATCH, Map.of(Dimension.KEYWORD_MATCH, 1.0));
		when(scoringProfileProvider.activeProfile(AnalysisMode.JOB_MATCH))
				.thenReturn(new ScoringProfileLookup(UUID.randomUUID(), scoringProfile));
		when(scoringService.score(eq(scoringProfile), any())).thenReturn(new ScoringOutcome(70, List.of(), List.of()));
		when(aiPort.structureResume(any(), any(), any()))
				.thenReturn(new AiResult<>(aiOutput, new AiUsage("gpt-4o-mini", 50, 25, BigDecimal.valueOf(0.002), false)));
		when(docxRenderer.render(any(), any(), any())).thenReturn("docx".getBytes());
		when(pdfRenderer.render(any(), any(), any())).thenReturn("pdf".getBytes());
		when(resumeService.storeAdapted(eq(sourceVersionId), any(), anyString(), any(), anyString(), anyString(),
				anyString()))
				.thenReturn(new ResumeVersionCreated(resumeId, UUID.randomUUID(), "key-docx", 4, "sha", null));
		return parsingResult;
	}

	private RewriteServiceImpl newService() {
		return new RewriteServiceImpl(
				analysisSnapshotPort, parsingPipeline, resumeService, aiPort, rateLimiter, rewriteRepository,
				new ResumeDocumentWriter(docxRenderer, pdfRenderer, resumeService, contentStore), scoringService, scoringProfileProvider, jobStructuringService,
				sensitiveDataDetector, new BulletTextExtractor(), jobKeywordMatcher,
				new RewriteGuardRails(job -> text -> java.util.Set.of(), new BulletTextExtractor()),
				new RequirementEvidenceFinder(aiPort), new RewriteProgressTracker());
	}
}
