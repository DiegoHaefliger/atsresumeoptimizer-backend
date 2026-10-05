package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.TechVocabulary;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class KnownTermVocabulary implements TechVocabulary {

	private final Map<String, List<Pattern>> spellingsByCanonical;

	KnownTermVocabulary(Map<String, List<Pattern>> spellingsByCanonical) {
		this.spellingsByCanonical = Map.copyOf(spellingsByCanonical);
	}

	@Override
	public Set<String> termsIn(String text) {
		if (text == null || text.isBlank()) {
			return Set.of();
		}
		String normalized = NormalizedText.of(text);
		return spellingsByCanonical.entrySet().stream()
				.filter(entry -> entry.getValue().stream().anyMatch(pattern -> pattern.matcher(normalized).find()))
				.map(Map.Entry::getKey)
				.collect(Collectors.toUnmodifiableSet());
	}
}
