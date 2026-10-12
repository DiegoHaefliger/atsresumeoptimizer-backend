package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import java.util.List;
import org.junit.jupiter.api.Test;

class CertificationLineFormatterTest {

	@Test
	void splitsTitleAndYearWithoutBold() {
		List<TextSpan> spans = CertificationLineFormatter.format(List.of(new TextSpan("Java SE — Oracle — 2024", false)));

		assertThat(TextSpan.plainText(spans)).isEqualTo("Java SE — Oracle | 2024");
		assertThat(spans).noneMatch(TextSpan::bold);
	}

	@Test
	void removesBoldTheAiAlreadyApplied() {
		List<TextSpan> spans = CertificationLineFormatter
				.format(List.of(new TextSpan("Java SE — Oracle", true), new TextSpan(" | 2024", false)));

		assertThat(TextSpan.plainText(spans)).isEqualTo("Java SE — Oracle | 2024");
		assertThat(spans).noneMatch(TextSpan::bold);
	}
}
