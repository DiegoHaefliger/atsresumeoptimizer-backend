package com.diegohaefliger.atsresumeoptimizer.ai.application;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;

/** Preço aproximado, só pra comparar custo entre modelos; não é a fatura do provedor. */
final class ModelPricing {

	private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);
	private static final int COST_SCALE = 4;
	private static final Price DEFAULT_PRICE = perMillion(0.15, 0.60);

	private static final Map<String, Price> PRICES_USD_PER_TOKEN = Map.ofEntries(
			Map.entry("gpt-4o-mini", DEFAULT_PRICE),
			Map.entry("gpt-4o", perMillion(2.50, 10.00)),
			Map.entry("gpt-4.1-nano", perMillion(0.10, 0.40)),
			Map.entry("gpt-4.1-mini", perMillion(0.40, 1.60)),
			Map.entry("gpt-4.1", perMillion(2.00, 8.00)),
			Map.entry("claude-fable-5-1", perMillion(10.00, 50.00)),
			Map.entry("claude-opus-5-5", perMillion(4.00, 20.00)),
			Map.entry("claude-opus-5", perMillion(5.00, 25.00)),
			Map.entry("claude-opus-4-8", perMillion(5.00, 25.00)),
			Map.entry("claude-opus-4-7", perMillion(5.00, 25.00)),
			Map.entry("claude-opus-4-6", perMillion(5.00, 25.00)),
			Map.entry("claude-sonnet-5-5", perMillion(2.00, 10.00)),
			Map.entry("claude-sonnet-5", perMillion(2.00, 10.00)),
			Map.entry("claude-sonnet-4-6", perMillion(3.00, 15.00)),
			Map.entry("claude-haiku-4-5", perMillion(1.00, 5.00)),
			Map.entry("gemini-2.5-flash", perMillion(0.30, 2.50)),
			Map.entry("gemini-2.5-pro", perMillion(1.25, 10.00)));

	private ModelPricing() {
	}

	static BigDecimal costUsd(AiProvider provider, String model, int tokensIn, int tokensOut) {
		if (provider == AiProvider.OLLAMA) {
			return BigDecimal.ZERO;
		}
		Price price = PRICES_USD_PER_TOKEN.getOrDefault(model, DEFAULT_PRICE);
		BigDecimal inputCost = price.perInputToken().multiply(BigDecimal.valueOf(tokensIn));
		BigDecimal outputCost = price.perOutputToken().multiply(BigDecimal.valueOf(tokensOut));
		return inputCost.add(outputCost).setScale(COST_SCALE, RoundingMode.HALF_UP);
	}

	private static Price perMillion(double input, double output) {
		return new Price(BigDecimal.valueOf(input).divide(ONE_MILLION, MathContext.DECIMAL64),
				BigDecimal.valueOf(output).divide(ONE_MILLION, MathContext.DECIMAL64));
	}

	private record Price(BigDecimal perInputToken, BigDecimal perOutputToken) {
	}
}
