package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.AiCallException;
import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsage;
import com.diegohaefliger.atsresumeoptimizer.ai.JobFocus;
import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.ai.PrioritySkill;
import com.diegohaefliger.atsresumeoptimizer.ai.RequirementEvidence;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisSnapshot;
import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisSnapshotPort;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisStatus;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringResult;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.parsing.application.BulletTextExtractor;
import com.diegohaefliger.atsresumeoptimizer.parsing.application.ResumeAnalysisPipeline;
import com.diegohaefliger.atsresumeoptimizer.parsing.application.SensitiveDataDetector;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.BulletRewriteView;
import com.diegohaefliger.atsresumeoptimizer.rewrite.OriginalSection;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RemovedSkillView;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeContact;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewritePhase;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewriteResult;
import com.diegohaefliger.atsresumeoptimizer.rewrite.RewriteService;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.AnalysisNotReadyForRewriteException;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.JobHighlight;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.RewriteFailedException;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.RewriteRateLimitExceededException;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Finding;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.FindingCode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.JobKeywordMatcher;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.KeywordMatch;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringContext;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringOutcome;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringProfileLookup;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringProfileProvider;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringService;
import com.github.f4b6a3.uuid.UuidCreator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
class RewriteServiceImpl implements RewriteService {

	private static final Logger LOGGER = LoggerFactory.getLogger(RewriteServiceImpl.class);
	private static final int MAX_BULLETS_PREVIEWED = 15;
	private static final String DROPPED_CONTENT_RETRY_INSTRUCTION = """

			- ATENÇÃO: a resposta anterior foi rejeitada porque %s. Devolva TODOS os cargos, projetos e bullets do \
			currículo original, cada bullet no seu cargo; destacar pra vaga é só reordenar, nunca cortar.
			""";
	private static final String UNCOVERED_SKILLS_RETRY_INSTRUCTION = """

			- ATENÇÃO: a resposta anterior não citou %s em nenhum bullet de experiência. O candidato JÁ usou isso \
			nesses cargos: reescreva o bullet em que aparece (ou o contexto do cargo que já cita) deixando a competência \
			explícita, sem inventar fato nem levá-la pra outro cargo.
			""";
	private static final String UNTITLED_JOB = "Vaga informada pelo candidato";
	private static final Set<FindingCode> CORRECTABLE_BY_REWRITE =
			Set.of(FindingCode.WEAK_ACTION_VERB, FindingCode.SECTION_TITLE_NOT_RECOGNIZED);

	private final AnalysisSnapshotPort analysisSnapshotPort;
	private final ResumeAnalysisPipeline parsingPipeline;
	private final ResumeService resumeService;
	private final AiPort aiPort;
	private final RewriteRateLimiter rateLimiter;
	private final RewriteRepository rewriteRepository;
	private final ResumeDocumentWriter documentWriter;
	private final ScoringService scoringService;
	private final ScoringProfileProvider scoringProfileProvider;
	private final JobStructuringService jobStructuringService;
	private final SensitiveDataDetector sensitiveDataDetector;
	private final BulletTextExtractor bulletTextExtractor;
	private final JobKeywordMatcher jobKeywordMatcher;
	private final RewriteGuardRails guardRails;
	private final RequirementEvidenceFinder requirementEvidenceFinder;
	private final RewriteProgressTracker progressTracker;
	private final BulletPairing bulletPairing = new BulletPairing();

	RewriteServiceImpl(
			AnalysisSnapshotPort analysisSnapshotPort,
			ResumeAnalysisPipeline parsingPipeline,
			ResumeService resumeService,
			AiPort aiPort,
			RewriteRateLimiter rateLimiter,
			RewriteRepository rewriteRepository,
			ResumeDocumentWriter documentWriter,
			ScoringService scoringService,
			ScoringProfileProvider scoringProfileProvider,
			JobStructuringService jobStructuringService,
			SensitiveDataDetector sensitiveDataDetector,
			BulletTextExtractor bulletTextExtractor,
			JobKeywordMatcher jobKeywordMatcher,
			RewriteGuardRails guardRails,
			RequirementEvidenceFinder requirementEvidenceFinder,
			RewriteProgressTracker progressTracker) {
		this.analysisSnapshotPort = analysisSnapshotPort;
		this.parsingPipeline = parsingPipeline;
		this.resumeService = resumeService;
		this.aiPort = aiPort;
		this.rateLimiter = rateLimiter;
		this.rewriteRepository = rewriteRepository;
		this.documentWriter = documentWriter;
		this.scoringService = scoringService;
		this.scoringProfileProvider = scoringProfileProvider;
		this.jobStructuringService = jobStructuringService;
		this.sensitiveDataDetector = sensitiveDataDetector;
		this.bulletTextExtractor = bulletTextExtractor;
		this.jobKeywordMatcher = jobKeywordMatcher;
		this.guardRails = guardRails;
		this.requirementEvidenceFinder = requirementEvidenceFinder;
		this.progressTracker = progressTracker;
	}

	@Override
	@Transactional
	public RewriteResult rewrite(AnalysisId analysisId, ResumeTemplate template, JobHighlight jobHighlight) {
		try {
			return doRewrite(analysisId, template, jobHighlight);
		} finally {
			progressTracker.finish(analysisId.value());
		}
	}

	private RewriteResult doRewrite(AnalysisId analysisId, ResumeTemplate template, JobHighlight jobHighlight) {
		AnalysisSnapshot snapshot =
				analysisSnapshotPort.find(analysisId).orElseThrow(() -> new AnalysisNotFoundException(analysisId));
		if (snapshot.status() != AnalysisStatus.COMPLETED && snapshot.status() != AnalysisStatus.PARTIAL) {
			throw new AnalysisNotReadyForRewriteException();
		}
		if (!rateLimiter.tryConsume()) {
			throw new RewriteRateLimitExceededException();
		}

		progressTracker.advance(analysisId.value(), RewritePhase.READING);
		ParsingResult parsingResult = parsingPipeline.analyze(resumeService.downloadContent(snapshot.resumeVersionId()));
		ScoringProfileLookup profileLookup = scoringProfileProvider.activeProfile(snapshot.mode());
		JobStructured job = resolveJob(snapshot);

		String correctionInstructions = correctionInstructions(parsingResult, profileLookup);
		ResumeHeader header = ResumeHeader.of(parsingResult);
		String originalText = fullText(parsingResult, header);
		List<AiUsage> extraUsages = new ArrayList<>();
		progressTracker.advance(analysisId.value(), RewritePhase.REWRITING);
		JobFocus jobFocus = jobFocus(jobHighlight, job, snapshot, parsingResult, originalText, extraUsages);
		AiResult<StructuredResume> structureResult = structure(originalText, correctionInstructions, jobFocus);
		progressTracker.advance(analysisId.value(), RewritePhase.CHECKING);
		StructuredResume guarded;
		try {
			guarded = enforceGuardRails(structureResult, originalText, parsingResult, job, jobFocus);
		} catch (DroppedContentException firstAttempt) {
			LOGGER.warn("Reescrita: {}, pedindo de novo à IA", firstAttempt.getMessage());
			extraUsages.add(structureResult.usage());
			structureResult = structure(originalText,
					correctionInstructions + DROPPED_CONTENT_RETRY_INSTRUCTION.formatted(firstAttempt.getMessage()), jobFocus);
			try {
				guarded = enforceGuardRails(structureResult, originalText, parsingResult, job, jobFocus);
			} catch (DroppedContentException secondAttempt) {
				throw new RewriteFailedException(secondAttempt.getMessage());
			}
		}
		List<PrioritySkill> uncovered = PrioritySkillCoverage.missing(guarded, jobFocus.prioritySkills());
		if (!uncovered.isEmpty()) {
			LOGGER.info("Reescrita: competências prioritárias fora dos bullets, pedindo de novo à IA: {}", uncovered);
			extraUsages.add(structureResult.usage());
			structureResult = structure(originalText,
					correctionInstructions + UNCOVERED_SKILLS_RETRY_INSTRUCTION.formatted(skillNames(uncovered)), jobFocus);
			try {
				guarded = enforceGuardRails(structureResult, originalText, parsingResult, job, jobFocus);
			} catch (DroppedContentException retryFailure) {
				LOGGER.warn("Reescrita: segunda tentativa de destacar competências foi rejeitada: {}", retryFailure.getMessage());
			}
		}
		StructuredResume ordered =
				new JobRelevanceOrdering(relevanceKeywords(jobFocus)).apply(RedundantParentheticalCleaner.apply(guarded));
		StructuredResume contentModel = StructuredResumeSanitizer.sanitizeGrammar(
				CanonicalSections.apply(ordered).withName(header.resolveName(ordered.name(), originalText)));
		List<BulletRewriteView> views = buildBulletViews(parsingResult, contentModel);

		ResumeContact contact = ParsedResumeImporter.contact(parsingResult);
		progressTracker.advance(analysisId.value(), RewritePhase.EXPORTING);
		StoredResumeDocuments documents = documentWriter.writeAdapted(snapshot.resumeVersionId(), analysisId.value(),
				AdaptedResumeTitle.suffix(job, snapshot), template, contentModel, contact);

		AiUsage usage = total(structureResult.usage(), extraUsages);
		rewriteRepository.save(new RewriteEntity(UuidCreator.getTimeOrderedEpoch(), analysisId.value(),
				documents.docxVersionId(), documents.pdfVersionId(), usage.model(), usage.tokensIn(), usage.tokensOut(),
				usage.costUsd(), jobFocus.enabled()));

		progressTracker.advance(analysisId.value(), RewritePhase.SCORING);
		Integer scoreAfter = scoreRewrittenResume(documents.docxBytes(), profileLookup, job);
		resumeService.updateAtsScore(documents.resumeId(), scoreAfter);

		return new RewriteResult(documents.resumeId(), documents.docxVersionId(), documents.pdfVersionId(), views,
				snapshot.overallScore(), scoreAfter, jobFocus.enabled(), removedSkills(contentModel), contentModel, contact,
				originalSections(parsingResult), jobFocus.evidencedRequirements(), usage.provider(), usage.model());
	}

	private StructuredResume enforceGuardRails(AiResult<StructuredResume> structureResult, String originalText,
			ParsingResult parsingResult, JobStructured job, JobFocus jobFocus) {
		return guardRails.enforce(structureResult.value(), originalText, parsingResult.sections(), job, jobFocus.enabled(),
				jobFocus.evidencedRequirements());
	}

	private AiResult<StructuredResume> structure(String originalText, String correctionInstructions, JobFocus jobFocus) {
		try {
			return aiPort.structureResume(originalText, correctionInstructions, jobFocus);
		} catch (AiCallException exception) {
			throw new RewriteFailedException(exception);
		}
	}

	private JobFocus jobFocus(JobHighlight jobHighlight, JobStructured job, AnalysisSnapshot snapshot,
			ParsingResult parsingResult, String originalText, List<AiUsage> extraUsages) {
		if (jobHighlight == JobHighlight.DISABLED || job == null) {
			return JobFocus.NONE;
		}
		List<KeywordMatch> matches = jobKeywordMatcher.match(parsingResult, job);
		List<String> matchedKeywords = matches.stream().filter(KeywordMatch::found).map(KeywordMatch::term).toList();
		List<String> missingRequired = matches.stream()
				.filter(match -> match.required() && !match.found())
				.map(KeywordMatch::term)
				.toList();
		List<RequirementEvidence> evidences = requirementEvidenceFinder.find(originalText, missingRequired)
				.map(result -> {
					extraUsages.add(result.usage());
					return result.value();
				})
				.orElse(List.of());
		return new JobFocus(jobTitle(job, snapshot), job.seniority(), matchedKeywords, evidences,
				PrioritySkillSelector.select(matches, job.priorityKeywords(), evidences, parsingResult.sections()));
	}

	private static String skillNames(List<PrioritySkill> skills) {
		return skills.stream().map(PrioritySkill::term).collect(Collectors.joining(", "));
	}

	private String jobTitle(JobStructured job, AnalysisSnapshot snapshot) {
		if (StringUtils.hasText(job.title())) {
			return job.title();
		}
		return StringUtils.hasText(snapshot.targetRole()) ? snapshot.targetRole() : UNTITLED_JOB;
	}

	private List<RemovedSkillView> removedSkills(StructuredResume content) {
		return content.removedSkills().stream().map(removed -> new RemovedSkillView(removed.skill(), removed.reason())).toList();
	}

	/** KEYWORD_REQUIRED_MISSING fica de fora: testado com vaga real, a IA inventa a tecnologia mesmo com "se você tiver". */
	private String correctionInstructions(ParsingResult parsingResult, ScoringProfileLookup profileLookup) {
		ScoringContext scoringContext =
				new ScoringContext(parsingResult, parsingResult.document().pageCount(), null, List.of());
		ScoringOutcome outcome = scoringService.score(profileLookup.profile(), scoringContext);
		List<Finding> findings = outcome.dimensions().stream()
				.flatMap(dimension -> dimension.findings().stream())
				.filter(finding -> CORRECTABLE_BY_REWRITE.contains(finding.code()))
				.toList();
		if (findings.isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (Finding finding : findings) {
			builder.append("- ").append(finding.message());
			if (StringUtils.hasText(finding.suggestion())) {
				builder.append(" Sugestão: ").append(finding.suggestion());
			}
			builder.append('\n');
		}
		return builder.toString();
	}

	private Integer scoreRewrittenResume(byte[] rewrittenDocxBytes, ScoringProfileLookup profileLookup, JobStructured job) {
		ParsingResult rewrittenParsing = parsingPipeline.analyze(rewrittenDocxBytes);
		ScoringContext scoringContext =
				new ScoringContext(rewrittenParsing, rewrittenParsing.document().pageCount(), job, List.of());
		ScoringOutcome outcome = scoringService.score(profileLookup.profile(), scoringContext);
		return outcome.overallScore();
	}

	private JobStructured resolveJob(AnalysisSnapshot snapshot) {
		try {
			if (snapshot.mode() == AnalysisMode.JOB_MATCH && StringUtils.hasText(snapshot.jobDescription())) {
				JobStructuringResult result = jobStructuringService.structureFromText(snapshot.jobDescription());
				return result.structured();
			}
			if (StringUtils.hasText(snapshot.targetRole())) {
				JobStructuringResult result = jobStructuringService.structureFromTargetRole(snapshot.targetRole());
				return result.structured();
			}
		} catch (AiCallException exception) {
			return null;
		}
		return null;
	}

	/** Preview vem do documento final, pareado por texto: o que aparece é o que o usuário baixa. */
	private List<BulletRewriteView> buildBulletViews(ParsingResult parsingResult, StructuredResume contentModel) {
		List<String> originalBullets = extractBullets(parsingResult).stream().map(bulletTextExtractor::stripMarker).toList();
		return bulletPairing.pair(originalBullets, flattenBullets(contentModel)).stream()
				.map(pair -> new BulletRewriteView(pair.original(), pair.rewritten(),
						FactGroundingChecker.introducesNewNumbers(pair.original(), pair.rewritten())))
				.toList();
	}

	private List<String> flattenBullets(StructuredResume content) {
		List<String> bullets = new ArrayList<>();
		for (ResumeSection section : content.sections()) {
			for (ResumeEntry entry : section.entries()) {
				entry.bullets().forEach(bullet -> bullets.add(TextSpan.plainText(bullet)));
			}
		}
		return bullets;
	}

	private String fullText(ParsingResult parsingResult, ResumeHeader header) {
		StringBuilder builder = new StringBuilder();
		header.lines().forEach(line -> builder.append(line).append('\n'));
		for (Section section : parsingResult.sections()) {
			builder.append(section.title()).append('\n').append(section.content()).append('\n');
		}
		return sensitiveDataDetector.mask(builder.toString());
	}

	private List<String> relevanceKeywords(JobFocus jobFocus) {
		List<String> keywords = new ArrayList<>(jobFocus.matchedKeywords());
		jobFocus.evidencedRequirements().forEach(evidence -> {
			keywords.add(evidence.requirement());
			keywords.add(evidence.evidence());
		});
		return keywords;
	}

	private AiUsage total(AiUsage main, List<AiUsage> extras) {
		return extras.stream().reduce(main, AiUsage::plus);
	}

	private List<OriginalSection> originalSections(ParsingResult parsingResult) {
		return parsingResult.sections().stream()
				.map(section -> new OriginalSection(section.title(), section.content()))
				.toList();
	}

	private List<String> extractBullets(ParsingResult parsingResult) {
		return bulletTextExtractor.extract(parsingResult.sections()).stream()
				.map(sensitiveDataDetector::mask)
				.limit(MAX_BULLETS_PREVIEWED)
				.toList();
	}
}
