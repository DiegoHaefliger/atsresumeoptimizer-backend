package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RedundantParentheticalCleanerTest {

	@Test
	void dropsAParentheticalThatOnlyRepeatsThePreviousWordWithAnotherSpelling() {
		assertThat(RedundantParentheticalCleaner.clean("Desenvolvimento de microsserviços (microserviços) back-end."))
				.isEqualTo("Desenvolvimento de microsserviços back-end.");
	}

	@Test
	void keepsAParentheticalThatAddsInformation() {
		assertThat(RedundantParentheticalCleaner.clean("Docker, Kafka (tecnologias de mensageria) e AWS."))
				.isEqualTo("Docker, Kafka (tecnologias de mensageria) e AWS.");
		assertThat(RedundantParentheticalCleaner.clean("Squad na Porto (remoto)")).isEqualTo("Squad na Porto (remoto)");
	}
}
