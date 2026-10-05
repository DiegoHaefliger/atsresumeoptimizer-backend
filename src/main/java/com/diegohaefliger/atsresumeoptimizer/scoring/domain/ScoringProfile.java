package com.diegohaefliger.atsresumeoptimizer.scoring.domain;

import java.util.Map;

public record ScoringProfile(String name, int version, AnalysisMode mode, Map<Dimension, Double> weights) {

	private static final double WEIGHT_SUM_TOLERANCE = 0.001;

	public ScoringProfile {
		weights = Map.copyOf(weights);
		double sum = weights.values().stream().mapToDouble(Double::doubleValue).sum();
		if (Math.abs(sum - 1.0) > WEIGHT_SUM_TOLERANCE) {
			throw new IllegalArgumentException("Pesos do scoring_profile devem somar 100%%, somaram %.4f".formatted(sum));
		}
	}

	public double weightOf(Dimension dimension) {
		return weights.getOrDefault(dimension, 0.0);
	}
}
