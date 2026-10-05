package com.diegohaefliger.atsresumeoptimizer.scoring.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SpellingVariantTest {

	@Test
	void treatsALongWordWithOneLetterOfDifferenceAsTheSameWord() {
		assertThat(SpellingVariant.same("microserviços", "Microsserviços")).isTrue();
		assertThat(SpellingVariant.containedIn("arquitetura de microsserviços e apis", "microserviços")).isTrue();
	}

	@Test
	void doesNotMixShortOrDifferentWords() {
		assertThat(SpellingVariant.same("Java", "Jave")).isFalse();
		assertThat(SpellingVariant.same("mensageria", "Kafka")).isFalse();
		assertThat(SpellingVariant.containedIn("bancos de dados relacionais", "bancos de dados nao relacionais")).isFalse();
	}
}
