package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.ai.AiCallException;
import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsage;
import com.diegohaefliger.atsresumeoptimizer.ai.BulletReview;
import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisCompleted;
import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisRequested;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisStatus;
import com.diegohaefliger.atsresumeoptimizer.job.JobDetails;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringResult;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.parsing.application.BulletTextExtractor;
import com.diegohaefliger.atsresumeoptimizer.parsing.application.ResumeAnalysisPipeline;
import com.diegohaefliger.atsresumeoptimizer.parsing.application.SensitiveDataDetector;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionCreated;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringContext;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringOutcome;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringProfileLookup;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringProfileProvider;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
class AnalysisServiceImpl implements AnalysisService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisServiceImpl.class);
	private static final int MAX_BULLETS_REVIEWED = 15;
	private static final String ERROR_SEPARATOR = "; ";

	private final ResumeAnalysisPipeline parsingPipeline;
	private final ResumeService resumeService;
	private final JobStructuringService jobStructuringService;
	private final ScoringService scoringService;
	private final ScoringProfileProvider scoringProfileProvider;
	private final AiPort aiPort;
	private final AnalysisRepository analysisRepository;
	private final AnalysisResultRecorder resultRecorder;
	private final AnalysisEventEmitterRegistry eventEmitterRegistry;
	private final ApplicationEventPublisher eventPublisher;
	private final SensitiveDataDetector sensitiveDataDetector;
	private final BulletTextExtractor bulletTextExtractor;

	AnalysisServiceImpl(
			ResumeAnalysisPipeline parsingPipeline,
			ResumeService resumeService,
			JobStructuringService jobStructuringService,
			ScoringService scoringService,
			ScoringProfileProvider scoringProfileProvider,
			AiPort aiPort,
			AnalysisRepository analysisRepository,
			AnalysisResultRecorder resultRecorder,
			AnalysisEventEmitterRegistry eventEmitterRegistry,
			ApplicationEventPublisher eventPublisher,
			SensitiveDataDetector sensitiveDataDetector,
			BulletTextExtractor bulletTextExtractor) {
		this.parsingPipeline = parsingPipeline;
		this.resumeService = resumeService;
		this.jobStructuringService = jobStructuringService;
		this.scoringService = scoringService;
		this.scoringProfileProvider = scoringProfileProvider;
		this.aiPort = aiPort;
		this.analysisRepository = analysisRepository;
		this.resultRecorder = resultRecorder;
		this.eventEmitterRegistry = eventEmitterRegistry;
		this.eventPublisher = eventPublisher;
		this.sensitiveDataDetector = sensitiveDataDetector;
		this.bulletTextExtractor = bulletTextExtractor;
	}

	@Override
	@Transactional
	public AnalysisCreation create(
			ResumeUpload resumeUpload, byte[] content, String jobDescription, String targetRole, JobDetails jobDetails) {
		ResumeVersionCreated resumeVersion =
				resumeService.upload(content, resumeUpload.fileName(), resumeUpload.contentType());
		return createAnalysis(resumeVersion.resumeVersionId(), registerJob(jobDescription, jobDetails), jobDescription,
				targetRole);
	}

	@Override
	@Transactional
	public AnalysisCreation createFromExistingVersion(UUID resumeId, UUID resumeVersionId, UUID jobPostingId,
			String jobDescription, String targetRole, JobDetails jobDetails) {
		resumeService.assertExists(resumeId, resumeVersionId);
		if (jobPostingId != null) {
			return createAnalysis(
					resumeVersionId, jobPostingId, jobStructuringService.getRawText(jobPostingId), targetRole);
		}
		return createAnalysis(resumeVersionId, registerJob(jobDescription, jobDetails), jobDescription, targetRole);
	}

	/** Roda depois do commit que gravou {@code PENDING}: é o que deixa {@code POST /analyses} responder 202 sem esperar a IA. */
	@ApplicationModuleListener
	void onAnalysisRequested(AnalysisRequested event) {
		AnalysisEntity entity = analysisRepository.findById(event.analysisId())
				.orElseThrow(() -> new IllegalStateException("Analysis não encontrada: " + event.analysisId()));
		if (entity.status() != AnalysisStatus.PENDING) {
			return;
		}

		AnalysisStatus status;
		try {
			advance(entity, entity::startParsing);
			ParsingResult parsingResult = parsingPipeline.analyze(resumeService.downloadContent(entity.resumeVersionId()));
			advance(entity, entity::startAnalyzing);
			status = runExpensivePipeline(entity, parsingResult);
		} catch (RuntimeException exception) {
			LOGGER.warn("Pipeline da análise {} falhou de um jeito inesperado, marcando FAILED", entity.id(), exception);
			status = AnalysisStatus.FAILED;
			entity.complete(entity.jobPostingId(), status, null, null, AiUsage.NONE, exception.getMessage());
			analysisRepository.save(entity);
		}

		eventEmitterRegistry.publish(entity.id(), status);
		eventEmitterRegistry.complete(entity.id());
		eventPublisher.publishEvent(new AnalysisCompleted(entity.id(), status));
	}

	private UUID registerJob(String jobDescription, JobDetails jobDetails) {
		return StringUtils.hasText(jobDescription) ? jobStructuringService.register(jobDescription, jobDetails) : null;
	}

	private AnalysisCreation createAnalysis(
			UUID resumeVersionId, UUID jobPostingId, String jobDescription, String targetRole) {
		AnalysisMode mode = StringUtils.hasText(jobDescription) ? AnalysisMode.JOB_MATCH : AnalysisMode.GENERAL;
		AnalysisId id = AnalysisId.generate();
		AnalysisEntity entity =
				new AnalysisEntity(id.value(), resumeVersionId, mode, targetRole, jobDescription, Instant.now());
		entity.linkJobPosting(jobPostingId);

		analysisRepository.save(entity);
		eventPublisher.publishEvent(new AnalysisRequested(id.value()));
		return new AnalysisCreation(id, AnalysisStatus.PENDING);
	}

	private void advance(AnalysisEntity entity, Runnable transition) {
		transition.run();
		analysisRepository.save(entity);
		eventEmitterRegistry.publish(entity.id(), entity.status());
	}

	private AnalysisStatus runExpensivePipeline(AnalysisEntity entity, ParsingResult parsingResult) {
		List<String> errors = new ArrayList<>();
		JobStructuringResult jobResult = structureJob(entity, errors);
		AiUsage usage = jobResult != null ? AiUsage.NONE.plus(jobResult.usage()) : AiUsage.NONE;

		List<BulletReview> bulletReviews = List.of();
		List<String> bullets = extractBullets(parsingResult);
		if (!bullets.isEmpty()) {
			try {
				AiResult<List<BulletReview>> reviewResult = aiPort.reviewBullets(bullets);
				bulletReviews = reviewResult.value();
				usage = usage.plus(reviewResult.usage());
			} catch (AiCallException exception) {
				errors.add(exception.getMessage());
			}
		}

		ScoringProfileLookup profileLookup = scoringProfileProvider.activeProfile(entity.mode());
		ScoringContext scoringContext = new ScoringContext(parsingResult, parsingResult.document().pageCount(),
				jobResult != null ? jobResult.structured() : null, bulletReviews);
		ScoringOutcome outcome = scoringService.score(profileLookup.profile(), scoringContext);

		AnalysisStatus status = errors.isEmpty() ? AnalysisStatus.COMPLETED : AnalysisStatus.PARTIAL;
		entity.complete(jobResult != null ? jobResult.jobPostingId() : entity.jobPostingId(), status,
				outcome.overallScore(), profileLookup.id(), usage,
				errors.isEmpty() ? null : String.join(ERROR_SEPARATOR, errors));
		analysisRepository.save(entity);
		resultRecorder.record(entity.id(), outcome, profileLookup.profile());
		return status;
	}

	private JobStructuringResult structureJob(AnalysisEntity entity, List<String> errors) {
		if (StringUtils.hasText(entity.jobDescription())) {
			return tryStructureJob(() -> jobStructuringService.structureFromText(entity.jobDescription()), errors);
		}
		if (StringUtils.hasText(entity.targetRole())) {
			return tryStructureJob(() -> jobStructuringService.structureFromTargetRole(entity.targetRole()), errors);
		}
		return null;
	}

	private JobStructuringResult tryStructureJob(Supplier<JobStructuringResult> call, List<String> errors) {
		try {
			return call.get();
		} catch (AiCallException exception) {
			errors.add(exception.getMessage());
			return null;
		}
	}

	private List<String> extractBullets(ParsingResult parsingResult) {
		return bulletTextExtractor.extract(parsingResult.sections()).stream()
				.map(sensitiveDataDetector::mask)
				.limit(MAX_BULLETS_REVIEWED)
				.toList();
	}
}
