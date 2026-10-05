package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IconGlyphDetectorTest {

	private final IconGlyphDetector detector = new IconGlyphDetector();

	@Test
	void plainResumeTextIsNotFlagged() {
		String text = "Ana Silva - Desenvolvedora Backend Java. Email: ana@example.com. Telefone: (11) 99999-0000.";

		assertThat(detector.hasIconGlyphsWithoutUnicode(text)).isFalse();
	}

	@Test
	void flagsPrivateUseAreaIconUsedAsBullet() {
		String text = " Java\n Spring Boot\n PostgreSQL";

		assertThat(detector.hasIconGlyphsWithoutUnicode(text)).isTrue();
	}

	@Test
	void doesNotFlagRegularAccentedPortugueseText() {
		String text = "Experiência em atuação sênior com formação em Ciência da Computação.";

		assertThat(detector.hasIconGlyphsWithoutUnicode(text)).isFalse();
	}
}
