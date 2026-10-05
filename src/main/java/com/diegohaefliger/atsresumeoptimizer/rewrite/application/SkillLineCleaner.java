package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Anotação da vaga presa a um item ("PostgreSQL (bancos relacionais)") sugere que só aquele item conta. */
final class SkillLineCleaner {

	private static final Pattern PARENTHETICAL = Pattern.compile("\\s*\\(([^()]{1,80})\\)");
	private static final Pattern CONJUNCTION = Pattern.compile("\\s+(?:e|and)\\s+");
	private static final String LIST_SEPARATOR = ",";

	private final String normalizedOriginal;

	SkillLineCleaner(String originalText) {
		this.normalizedOriginal = NormalizedText.of(originalText);
	}

	StructuredResume apply(StructuredResume content) {
		return content.withSections(content.sections().stream()
				.map(section -> section.semanticType() == ResumeSectionSemanticType.SKILLS
						? section.withKeyValues(section.keyValues().stream()
								.map(line -> new KeyValueLine(line.label(), clean(line.value())))
								.toList())
						: section)
				.toList());
	}

	String clean(String value) {
		if (value == null || value.isBlank()) {
			return value;
		}
		return withoutDuplicates(withoutAddedAnnotations(value));
	}

	private String withoutAddedAnnotations(String value) {
		Matcher matcher = PARENTHETICAL.matcher(value);
		StringBuilder result = new StringBuilder();
		int last = 0;
		while (matcher.find()) {
			if (!NormalizedText.containsWord(normalizedOriginal, NormalizedText.of(matcher.group(1)))) {
				result.append(value, last, matcher.start());
				last = matcher.end();
			}
		}
		return result.append(value.substring(last)).toString();
	}

	private String withoutDuplicates(String value) {
		Map<String, String> items = new LinkedHashMap<>();
		Arrays.stream(value.split(LIST_SEPARATOR))
				.map(String::strip)
				.filter(item -> !item.isEmpty())
				.forEach(item -> items.putIfAbsent(NormalizedText.of(item), item));
		return items.values().stream()
				.filter(item -> !repeatsListedParts(item, items))
				.collect(Collectors.joining(LIST_SEPARATOR + " "));
	}

	private boolean repeatsListedParts(String item, Map<String, String> items) {
		String[] parts = CONJUNCTION.split(item);
		return parts.length > 1 && Arrays.stream(parts).allMatch(part -> items.containsKey(NormalizedText.of(part)));
	}
}
