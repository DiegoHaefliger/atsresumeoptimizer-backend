package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class BulletPairingTest {

	private final BulletPairing pairing = new BulletPairing();

	@Test
	void pairsEachOriginalWithItsRewriteEvenWhenTheOrderChanged() {
		List<String> originals = List.of("Criei testes automatizados em Java", "Migrei serviços para Kubernetes");
		List<String> finals = List.of("Migrei 12 serviços para Kubernetes com zero downtime", "Criei testes automatizados em Java e JUnit");

		assertThat(pairing.pair(originals, finals)).containsExactly(
				new BulletPairing.Pair("Criei testes automatizados em Java", "Criei testes automatizados em Java e JUnit"),
				new BulletPairing.Pair("Migrei serviços para Kubernetes", "Migrei 12 serviços para Kubernetes com zero downtime"));
	}

	@Test
	void fallsBackToPositionWhenTheRewriteSharesNoWordWithTheOriginal() {
		assertThat(pairing.pair(List.of("Responsável por QA"), List.of("Reduzi bugs em 40%")))
				.containsExactly(new BulletPairing.Pair("Responsável por QA", "Reduzi bugs em 40%"));
	}

	@Test
	void leavesOutOriginalsWithoutAnyRewriteLeft() {
		assertThat(pairing.pair(List.of("Primeiro bullet", "Segundo bullet"), List.of("Primeiro bullet revisado")))
				.containsExactly(new BulletPairing.Pair("Primeiro bullet", "Primeiro bullet revisado"));
	}
}
