package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static com.diegohaefliger.atsresumeoptimizer.parsing.application.OdtTestFixture.odt;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;

class OdtTableDetectorTest {

	private final OdtTableDetector detector = new OdtTableDetector();

	@Test
	void detectsATableWithContent() throws Exception {
		byte[] bytes = odt().withParagraphs("Ana Silva").withTable("Java", "Spring Boot").build();

		assertThat(detector.hasTable(new ByteArrayInputStream(bytes))).isTrue();
	}

	@Test
	void noTableIsNotFlagged() throws Exception {
		byte[] bytes = odt().withParagraphs("Ana Silva").build();

		assertThat(detector.hasTable(new ByteArrayInputStream(bytes))).isFalse();
	}
}
