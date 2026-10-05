package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static com.diegohaefliger.atsresumeoptimizer.parsing.application.OdtTestFixture.odt;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;

class OdtMultiColumnDetectorTest {

	private final OdtMultiColumnDetector detector = new OdtMultiColumnDetector();

	@Test
	void detectsTwoOrMoreColumnsInThePageLayout() throws Exception {
		byte[] bytes = odt().withColumns(2).build();

		assertThat(detector.isMultiColumn(new ByteArrayInputStream(bytes))).isTrue();
	}

	@Test
	void singleColumnIsNotFlagged() throws Exception {
		byte[] bytes = odt().withColumns(1).build();

		assertThat(detector.isMultiColumn(new ByteArrayInputStream(bytes))).isFalse();
	}
}
