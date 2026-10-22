package com.diegohaefliger.atsresumeoptimizer.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InlineMarkupTest {

	@Test
	void removesBoldUnderlineAndItalicMarks() {
		assertThat(InlineMarkup.strip("**Vale** ++Refeição++ e *Plano* de saúde")).isEqualTo("Vale Refeição e Plano de saúde");
	}

	@Test
	void keepsLoneAsterisksAndPlainText() {
		assertThat(InlineMarkup.strip("13º salário * proporcional")).isEqualTo("13º salário * proporcional");
		assertThat(InlineMarkup.strip("Plano de saúde")).isEqualTo("Plano de saúde");
	}

	@Test
	void theOfferExposesPlainBenefitsForComparisonAndTheMarkupForEditing() {
		JobOffer offer = new JobOffer(UUID.randomUUID(), 1L, "Dev", null, null, null, null, null, null, null,
				List.of("**Vale-refeição**", "Plano de saúde"), null, null, "texto");

		assertThat(offer.benefits()).containsExactly("Vale-refeição", "Plano de saúde");
		assertThat(offer.benefitsMarkup()).containsExactly("**Vale-refeição**", "Plano de saúde");
	}
}
