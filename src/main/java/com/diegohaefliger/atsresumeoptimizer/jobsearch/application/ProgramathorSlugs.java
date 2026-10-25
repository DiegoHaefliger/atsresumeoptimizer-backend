package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

final class ProgramathorSlugs {

	private static final int MAX_SLUGS = 2;
	private static final Set<String> GENERIC = Set.of("desenvolvedor", "desenvolvedora", "developer", "engenheiro",
			"engenheira", "engineer", "analista", "programador", "software", "de", "da", "do", "e", "em", "para", "pleno",
			"senior", "junior", "sr", "jr", "pl", "dev", "backend", "frontend", "fullstack", "full", "stack");

	private ProgramathorSlugs() {
	}

	static List<String> of(String keyword) {
		return Arrays.stream(NormalizedText.of(keyword).split("[^a-z0-9+#.]+"))
				.filter(word -> !word.isBlank() && !GENERIC.contains(word))
				.distinct()
				.limit(MAX_SLUGS)
				.toList();
	}
}
