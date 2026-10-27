package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.ai.AiCallException;
import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatImage;
import com.diegohaefliger.atsresumeoptimizer.ai.LinkedInAnalysisBrief;
import com.diegohaefliger.atsresumeoptimizer.ai.LinkedInAnalysisDraft;
import com.diegohaefliger.atsresumeoptimizer.careerprofile.CareerProfileService;
import com.diegohaefliger.atsresumeoptimizer.careerprofile.CareerProfile;
import com.diegohaefliger.atsresumeoptimizer.careerprofile.CareerProfileNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysis;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysisNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysisReport;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysisSummary;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysisUnavailableException;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSectionReview;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshot;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshotNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.ProfileSection;
import com.github.f4b6a3.uuid.UuidCreator;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
class LinkedInAnalysisServiceImpl implements LinkedInAnalysisService {

	private static final Logger LOGGER = LoggerFactory.getLogger(LinkedInAnalysisServiceImpl.class);

	private final LinkedInAnalysisRepository repository;
	private final LinkedInAnalysisEntityMapper mapper;
	private final LinkedInAnalysisDraftMapper draftMapper;
	private final LinkedInSnapshotService snapshotService;
	private final CareerProfileService careerProfileService;
	private final AiPort aiPort;
	private final Clock clock;

	LinkedInAnalysisServiceImpl(LinkedInAnalysisRepository repository, LinkedInAnalysisEntityMapper mapper,
			LinkedInAnalysisDraftMapper draftMapper, LinkedInSnapshotService snapshotService,
			CareerProfileService careerProfileService, AiPort aiPort, Clock clock) {
		this.repository = repository;
		this.mapper = mapper;
		this.draftMapper = draftMapper;
		this.snapshotService = snapshotService;
		this.careerProfileService = careerProfileService;
		this.aiPort = aiPort;
		this.clock = clock;
	}

	@Override
	public LinkedInAnalysis analyze(UUID snapshotId) {
		LinkedInSnapshot snapshot = snapshotId == null
				? snapshotService.latest().orElseThrow(LinkedInSnapshotNotFoundException::new)
				: snapshotService.get(snapshotId);
		CareerProfile career = careerProfileService.current().orElseThrow(CareerProfileNotFoundException::new);
		String careerText = LinkedInPromptText.careerProfile(career);
		String profileText = LinkedInPromptText.profile(snapshot);
		List<ChatImage> images = images(snapshot);

		boolean imagesAnalyzed = !images.isEmpty();
		AiResult<LinkedInAnalysisDraft> result;
		try {
			result = ask(new LinkedInAnalysisBrief(careerText, profileText, images));
		} catch (LinkedInAnalysisUnavailableException withImagesFailed) {
			if (images.isEmpty()) {
				throw withImagesFailed;
			}
			LOGGER.warn("Análise do LinkedIn falhou com imagens, tentando só com o texto: {}", withImagesFailed.getMessage());
			imagesAnalyzed = false;
			result = ask(new LinkedInAnalysisBrief(careerText, profileText, List.of()));
		}

		LinkedInAnalysisReport report = report(result.value());
		if (report.sections().isEmpty() || !StringUtils.hasText(result.value().summary())) {
			throw new LinkedInAnalysisUnavailableException("A IA não devolveu a análise. Tenta gerar de novo.");
		}
		LinkedInAnalysisEntity entity = new LinkedInAnalysisEntity(UuidCreator.getTimeOrderedEpoch(), snapshot.id(), clock.instant());
		entity.setOverallScore(draftMapper.score(result.value().overallScore()));
		entity.setSummary(result.value().summary().strip());
		entity.setReport(report);
		entity.setImagesAnalyzed(imagesAnalyzed);
		entity.setAiModel(result.usage().model());
		entity.setTokensIn(result.usage().tokensIn());
		entity.setTokensOut(result.usage().tokensOut());
		entity.setCostUsd(result.usage().costUsd());
		return mapper.toDomain(repository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<LinkedInAnalysis> latest() {
		return repository.findFirstByOrderByCreatedAtDesc().map(mapper::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public LinkedInAnalysis get(UUID id) {
		return repository.findById(id).map(mapper::toDomain).orElseThrow(LinkedInAnalysisNotFoundException::new);
	}

	@Override
	@Transactional(readOnly = true)
	public List<LinkedInAnalysisSummary> history() {
		return repository.findAllByOrderByCreatedAtDesc().stream().map(mapper::toSummary).toList();
	}

	private AiResult<LinkedInAnalysisDraft> ask(LinkedInAnalysisBrief brief) {
		try {
			return aiPort.analyzeLinkedIn(brief);
		} catch (AiCallException failure) {
			throw new LinkedInAnalysisUnavailableException(failure.getMessage());
		}
	}

	private LinkedInAnalysisReport report(LinkedInAnalysisDraft draft) {
		LinkedInAnalysisReport mapped = draftMapper.toReport(draft);
		Map<ProfileSection, LinkedInSectionReview> bySection = new LinkedHashMap<>();
		mapped.sections().stream()
				.filter(review -> review != null && review.section() != null)
				.forEach(review -> bySection.putIfAbsent(review.section(), review));
		return new LinkedInAnalysisReport(mapped.targetFit(), mapped.topActions(), mapped.recruiterKeywords(),
				mapped.missingKeywords(), List.copyOf(bySection.values()));
	}

	private static List<ChatImage> images(LinkedInSnapshot snapshot) {
		List<ChatImage> images = new ArrayList<>();
		if (snapshot.hasPhoto()) {
			images.add(new ChatImage(snapshot.photoMimeType(), Base64.getEncoder().encodeToString(snapshot.photo())));
		}
		if (snapshot.hasBanner()) {
			images.add(new ChatImage(snapshot.bannerMimeType(), Base64.getEncoder().encodeToString(snapshot.banner())));
		}
		return images;
	}
}
