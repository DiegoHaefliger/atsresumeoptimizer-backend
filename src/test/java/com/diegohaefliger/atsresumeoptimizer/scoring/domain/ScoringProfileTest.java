package com.diegohaefliger.atsresumeoptimizer.scoring.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ScoringProfileTest {

	@Test
	void rejectsWeightsThatDoNotSumToOne() {
		Map<Dimension, Double> incompleteWeights = Map.of(Dimension.PARSEABILITY, 0.5);

		assertThatThrownBy(() -> new ScoringProfile("invalid", 1, AnalysisMode.GENERAL, incompleteWeights))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
