package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Dimension;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionResult;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.DimensionScorer;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.Finding;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.FindingCode;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringContext;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringOutcome;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringProfile;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.ScoringService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class ScoringServiceImpl implements ScoringService {

	private final Map<Dimension, DimensionScorer> scorersByDimension;

	ScoringServiceImpl(List<DimensionScorer> scorers) {
		this.scorersByDimension = scorers.stream().collect(Collectors.toMap(DimensionScorer::dimension, Function.identity()));
	}

	@Override
	public ScoringOutcome score(ScoringProfile profile, ScoringContext context) {
		List<DimensionResult> results = profile.weights().keySet().stream()
				.map(scorersByDimension::get)
				.map(scorer -> scorer.score(context))
				.toList();

		double weightedSum = results.stream()
				.mapToDouble(result -> result.score() * profile.weightOf(result.dimension()))
				.sum();

		List<FindingCode> blockers = results.stream()
				.flatMap(result -> result.findings().stream())
				.map(Finding::code)
				.filter(FindingCode::blocker)
				.distinct()
				.toList();

		return new ScoringOutcome((int) Math.round(weightedSum), results, blockers);
	}
}
