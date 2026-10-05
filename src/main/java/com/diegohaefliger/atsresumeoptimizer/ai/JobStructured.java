package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;
import java.util.Map;

/** {@code keywordEquivalents}: formas que o currículo pode usar no lugar da grafia exata da vaga (match semântico do D4). */
public record JobStructured(
		String title,
		String seniority,
		Integer minYearsExperience,
		String educationLevel,
		List<String> languages,
		List<String> requiredKeywords,
		Map<String, List<String>> keywordEquivalents,
		JobConditions conditions) {

	public JobStructured {
		languages = languages == null ? List.of() : List.copyOf(languages);
		requiredKeywords = requiredKeywords == null ? List.of() : List.copyOf(requiredKeywords);
		keywordEquivalents = keywordEquivalents == null ? Map.of() : Map.copyOf(keywordEquivalents);
		conditions = conditions == null ? JobConditions.NONE : conditions;
	}

	public JobStructured(
			String title,
			String seniority,
			Integer minYearsExperience,
			String educationLevel,
			List<String> languages,
			List<String> requiredKeywords,
			Map<String, List<String>> keywordEquivalents) {
		this(title, seniority, minYearsExperience, educationLevel, languages, requiredKeywords, keywordEquivalents, null);
	}
}
