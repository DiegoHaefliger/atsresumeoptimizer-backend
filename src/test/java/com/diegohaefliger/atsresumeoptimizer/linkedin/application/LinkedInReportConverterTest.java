package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LinkedInReportConverterTest {

	private final LinkedInReportConverter converter = new LinkedInReportConverter();

	@Test
	void readsReportsSavedBeforeChecksExisted() {
		var report = converter.convertToEntityAttribute("{\"targetFit\":\"ok\",\"topActions\":[\"a\"]}");

		assertThat(report.checks()).isEmpty();
		assertThat(report.topActions()).containsExactly("a");
	}

	@Test
	void readsChecksSavedWithTheOldDetailField() {
		var report = converter.convertToEntityAttribute(
				"{\"checks\":[{\"label\":\"X\",\"passed\":false,\"detail\":\"antigo\"}]}");

		assertThat(report.checks()).hasSize(1);
		assertThat(report.checks().get(0).label()).isEqualTo("X");
	}
}
