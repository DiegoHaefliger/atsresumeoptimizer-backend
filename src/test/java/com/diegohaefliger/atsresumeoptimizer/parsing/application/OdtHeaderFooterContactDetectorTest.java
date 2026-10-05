package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static com.diegohaefliger.atsresumeoptimizer.parsing.application.OdtTestFixture.odt;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;

class OdtHeaderFooterContactDetectorTest {

	private final OdtHeaderFooterContactDetector detector = new OdtHeaderFooterContactDetector();

	@Test
	void detectsContactInfoInTheHeader() throws Exception {
		byte[] bytes = odt().withHeader("ana.silva@email.com | (11) 91234-5678").build();

		assertThat(detector.hasContactInHeaderOrFooter(new ByteArrayInputStream(bytes))).isTrue();
	}

	@Test
	void noHeaderOrFooterIsNotFlagged() throws Exception {
		byte[] bytes = odt().withParagraphs("Ana Silva").build();

		assertThat(detector.hasContactInHeaderOrFooter(new ByteArrayInputStream(bytes))).isFalse();
	}
}
