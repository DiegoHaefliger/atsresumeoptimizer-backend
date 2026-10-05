package com.diegohaefliger.atsresumeoptimizer.scoring.domain;

import java.util.List;

public record KeywordMatch(
		String term, String category, boolean required, boolean foundExact, boolean foundSemantic, int occurrences,
		List<String> sections) {

	public KeywordMatch {
		sections = sections == null ? List.of() : List.copyOf(sections);
	}

	public boolean found() {
		return foundExact || foundSemantic;
	}
}
