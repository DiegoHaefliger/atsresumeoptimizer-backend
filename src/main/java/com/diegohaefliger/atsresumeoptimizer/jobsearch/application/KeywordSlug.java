package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import java.util.Arrays;
import java.util.stream.Collectors;

final class KeywordSlug {

	private KeywordSlug() {
	}

	static String of(String keyword) {
		return Arrays.stream(NormalizedText.of(keyword).split("[^a-z0-9]+"))
				.filter(word -> !word.isBlank())
				.collect(Collectors.joining("-"));
	}
}
