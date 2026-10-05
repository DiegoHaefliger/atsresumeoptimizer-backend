package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FactGroundingCheckerTest {

	@Test
	void flagsANumberThatDidNotExistInTheOriginalBullet() {
		boolean flagged = FactGroundingChecker.introducesNewNumbers(
				"responsavel por testes", "Reduzi bugs em 40% através de testes automatizados");

		assertThat(flagged).isTrue();
	}

	@Test
	void doesNotFlagWhenTheRewriteReusesTheSameNumberFromTheOriginal() {
		boolean flagged = FactGroundingChecker.introducesNewNumbers(
				"Liderei equipe de 5 pessoas", "Liderei uma equipe de 5 desenvolvedores");

		assertThat(flagged).isFalse();
	}

	@Test
	void doesNotFlagWhenNeitherHasNumbers() {
		boolean flagged = FactGroundingChecker.introducesNewNumbers("responsavel por testes", "Executei testes automatizados");

		assertThat(flagged).isFalse();
	}
}
