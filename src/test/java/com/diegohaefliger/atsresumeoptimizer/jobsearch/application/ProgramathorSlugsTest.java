package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProgramathorSlugsTest {

	@Test
	void dropsGenericWordsAndKeepsTheTechnology() {
		assertThat(ProgramathorSlugs.of("Desenvolvedor Java Sênior")).containsExactly("java");
		assertThat(ProgramathorSlugs.of("Spring Boot")).containsExactly("spring", "boot");
	}

	@Test
	void keepsSymbolsThatBelongToTechnologyNames() {
		assertThat(ProgramathorSlugs.of("C# .NET")).containsExactly("c#", ".net");
	}

	@Test
	void returnsNothingForOnlyGenericWords() {
		assertThat(ProgramathorSlugs.of("Desenvolvedor Backend")).isEmpty();
	}
}
