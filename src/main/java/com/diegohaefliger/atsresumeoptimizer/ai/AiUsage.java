package com.diegohaefliger.atsresumeoptimizer.ai;

import java.math.BigDecimal;

/** {@code cached=true}: resposta veio do {@code llm_cache}, sem chamada nova ao provedor, então tokens e custo zerados. */
public record AiUsage(String provider, String model, int tokensIn, int tokensOut, BigDecimal costUsd, boolean cached) {

	public static final AiUsage NONE = new AiUsage(null, null, 0, 0, BigDecimal.ZERO, false);

	public AiUsage {
		costUsd = costUsd == null ? BigDecimal.ZERO : costUsd;
	}

	public AiUsage(String model, int tokensIn, int tokensOut, BigDecimal costUsd, boolean cached) {
		this(null, model, tokensIn, tokensOut, costUsd, cached);
	}

	public static AiUsage cached(String provider, String model) {
		return new AiUsage(provider, model, 0, 0, BigDecimal.ZERO, true);
	}

	public AiUsage plus(AiUsage other) {
		if (other == null) {
			return this;
		}
		AiUsage first = model != null ? this : other;
		return new AiUsage(first.provider, first.model, tokensIn + other.tokensIn, tokensOut + other.tokensOut,
				costUsd.add(other.costUsd), cached && other.cached);
	}
}
