package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KeywordSlugTest {

	@Test
	void joinsTheNormalizedWordsWithDashes() {
		assertThat(KeywordSlug.of("Desenvolvedor Java Sênior")).isEqualTo("desenvolvedor-java-senior");
		assertThat(KeywordSlug.of("  C# / .NET ")).isEqualTo("c-net");
		assertThat(KeywordSlug.of("   ")).isEmpty();
	}
}
