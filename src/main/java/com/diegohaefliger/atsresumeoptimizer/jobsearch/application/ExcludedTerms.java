package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class ExcludedTerms {

	private static final Pattern PUNCTUATION = Pattern.compile("[^\\p{L}\\p{N}]+");

	private final List<String> terms;

	ExcludedTerms(List<String> terms) {
		this.terms = terms.stream().map(ExcludedTerms::normalized).filter(term -> !term.isBlank()).toList();
	}

	boolean isEmpty() {
		return terms.isEmpty();
	}

	boolean appearIn(String... texts) {
		String text = normalized(Stream.of(texts).filter(Objects::nonNull).collect(Collectors.joining(" ")));
		return terms.stream().anyMatch(term -> NormalizedText.containsWord(text, term));
	}

	private static String normalized(String text) {
		return NormalizedText.of(PUNCTUATION.matcher(HtmlText.decoded(text)).replaceAll(" "));
	}
}
