package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionResult;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringOutcome;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringProfile;
import com.github.f4b6a3.uuid.UuidCreator;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class AnalysisResultRecorder {

	private final AnalysisDimensionRepository dimensionRepository;
	private final FindingRepository findingRepository;
	private final KeywordMatchRepository keywordMatchRepository;

	AnalysisResultRecorder(
			AnalysisDimensionRepository dimensionRepository,
			FindingRepository findingRepository,
			KeywordMatchRepository keywordMatchRepository) {
		this.dimensionRepository = dimensionRepository;
		this.findingRepository = findingRepository;
		this.keywordMatchRepository = keywordMatchRepository;
	}

	void record(UUID analysisId, ScoringOutcome outcome, ScoringProfile profile) {
		List<DimensionResult> dimensions = outcome.dimensions();
		dimensionRepository.saveAll(dimensions.stream()
				.map(result -> new AnalysisDimensionEntity(UuidCreator.getTimeOrderedEpoch(), analysisId, result.dimension(),
						result.score(), BigDecimal.valueOf(profile.weightOf(result.dimension()))))
				.toList());
		findingRepository.saveAll(dimensions.stream()
				.flatMap(result -> result.findings().stream()
						.map(finding -> new FindingEntity(UuidCreator.getTimeOrderedEpoch(), analysisId, result.dimension(),
								finding)))
				.toList());
		keywordMatchRepository.saveAll(dimensions.stream()
				.flatMap(result -> result.keywordMatches().stream())
				.map(match -> new KeywordMatchEntity(UuidCreator.getTimeOrderedEpoch(), analysisId, match))
				.toList());
	}
}
