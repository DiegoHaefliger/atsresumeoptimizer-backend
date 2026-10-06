package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;
import java.util.Map;

/** {@code priorityKeywords}: as principais, que o adapter destaca nas experiências; sem escolha do usuário, as 5 primeiras.
 * {@code keywordEquivalents}: formas que o currículo pode usar no lugar da grafia exata da vaga (match semântico do D4). */
public record JobStructured(
		String title,
		String seniority,
		Integer minYearsExperience,
		String educationLevel,
		List<String> languages,
		List<String> requiredKeywords,
		Map<String, List<String>> keywordEquivalents,
		JobConditions conditions,
		List<String> priorityKeywords) {

	public static final int DEFAULT_PRIORITY_COUNT = 5;

	public JobStructured {
		languages = languages == null ? List.of() : List.copyOf(languages);
		requiredKeywords = requiredKeywords == null ? List.of() : List.copyOf(requiredKeywords);
		keywordEquivalents = keywordEquivalents == null ? Map.of() : Map.copyOf(keywordEquivalents);
		conditions = conditions == null ? JobConditions.NONE : conditions;
		priorityKeywords = priorityKeywords == null
				? requiredKeywords.stream().limit(DEFAULT_PRIORITY_COUNT).toList()
				: List.copyOf(priorityKeywords);
	}

	public JobStructured(
			String title,
			String seniority,
			Integer minYearsExperience,
			String educationLevel,
			List<String> languages,
			List<String> requiredKeywords,
			Map<String, List<String>> keywordEquivalents,
			JobConditions conditions) {
		this(title, seniority, minYearsExperience, educationLevel, languages, requiredKeywords, keywordEquivalents,
				conditions, null);
	}

	public JobStructured(
			String title,
			String seniority,
			Integer minYearsExperience,
			String educationLevel,
			List<String> languages,
			List<String> requiredKeywords,
			Map<String, List<String>> keywordEquivalents) {
		this(title, seniority, minYearsExperience, educationLevel, languages, requiredKeywords, keywordEquivalents, null, null);
	}
}
