package com.diegohaefliger.atsresumeoptimizer.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ModelPricingTest {

	@Test
	void knowsTheModelsInTheTable() {
		assertThat(ModelPricing.hasPrice("gpt-4o-mini")).isTrue();
		assertThat(ModelPricing.hasPrice("gpt-5-mini")).isTrue();
		assertThat(ModelPricing.hasPrice("claude-sonnet-4-5")).isTrue();
	}

	@Test
	void pricesADatedModelIdByItsLongestKnownPrefix() {
		assertThat(ModelPricing.hasPrice("gpt-4.1-mini-2025-04-14")).isTrue();
		assertThat(ModelPricing.costUsd(AiProvider.OPENAI, "gpt-4.1-mini-2025-04-14", 1_000_000, 0))
				.isEqualByComparingTo(ModelPricing.costUsd(AiProvider.OPENAI, "gpt-4.1-mini", 1_000_000, 0))
				.isEqualByComparingTo(new BigDecimal("0.40"));
	}

	@Test
	void doesNotKnowAModelOutsideTheTable() {
		assertThat(ModelPricing.hasPrice("modelo-novo")).isFalse();
		assertThat(ModelPricing.hasPrice(null)).isFalse();
	}
}
