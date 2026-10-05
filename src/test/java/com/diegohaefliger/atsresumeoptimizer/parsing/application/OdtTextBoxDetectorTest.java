package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static com.diegohaefliger.atsresumeoptimizer.parsing.application.OdtTestFixture.odt;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;

class OdtTextBoxDetectorTest {

	private final OdtTextBoxDetector detector = new OdtTextBoxDetector();

	@Test
	void detectsTextOutsideTheMainFlow() throws Exception {
		byte[] bytes = odt().withParagraphs("Ana Silva").withTextBox("Resumo lateral").build();

		assertThat(detector.hasTextBox(new ByteArrayInputStream(bytes))).isTrue();
	}

	@Test
	void noTextBoxIsNotFlagged() throws Exception {
		byte[] bytes = odt().withParagraphs("Ana Silva").build();

		assertThat(detector.hasTextBox(new ByteArrayInputStream(bytes))).isFalse();
	}
}
