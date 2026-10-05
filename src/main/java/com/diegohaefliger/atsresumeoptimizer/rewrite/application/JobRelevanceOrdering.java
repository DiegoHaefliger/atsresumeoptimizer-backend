package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Dentro de cada cargo, o que bate com a vaga vem primeiro; ordenação estável, então o resto mantém a ordem da IA. */
final class JobRelevanceOrdering {

	private static final String LIST_SEPARATOR = ",";

	private final List<String> normalizedKeywords;

	JobRelevanceOrdering(List<String> matchedKeywords) {
		this.normalizedKeywords = matchedKeywords.stream().map(NormalizedText::of).filter(term -> !term.isBlank()).toList();
	}

	StructuredResume apply(StructuredResume content) {
		if (normalizedKeywords.isEmpty()) {
			return content;
		}
		return content.withSections(content.sections().stream()
				.map(section -> section.semanticType().holdsBullets()
						? section.withEntries(section.entries().stream().map(this::ordered).toList())
						: section)
				.toList());
	}

	private ResumeEntry ordered(ResumeEntry entry) {
		List<List<TextSpan>> bullets = entry.bullets().stream()
				.sorted(Comparator.comparingLong((List<TextSpan> bullet) -> matches(TextSpan.plainText(bullet))).reversed())
				.toList();
		return entry.withBulletsAndTechnologies(bullets, orderedTechnologies(entry.technologies()));
	}

	private String orderedTechnologies(String items) {
		if (items == null || items.isBlank()) {
			return items;
		}
		return Arrays.stream(items.split(LIST_SEPARATOR))
				.map(String::strip)
				.filter(item -> !item.isEmpty())
				.sorted(Comparator.comparingLong(this::matches).reversed())
				.collect(Collectors.joining(LIST_SEPARATOR + " "));
	}

	private long matches(String text) {
		String normalized = NormalizedText.of(text);
		return normalizedKeywords.stream().filter(keyword -> NormalizedText.containsWord(normalized, keyword)).count();
	}
}
