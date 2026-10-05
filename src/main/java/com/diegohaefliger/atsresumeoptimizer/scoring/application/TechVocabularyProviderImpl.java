package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.TechVocabulary;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.TechVocabularyProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
class TechVocabularyProviderImpl implements TechVocabularyProvider {

	private final TechTermRepository techTermRepository;

	TechVocabularyProviderImpl(TechTermRepository techTermRepository) {
		this.techTermRepository = techTermRepository;
	}

	@Override
	public TechVocabulary forJob(JobStructured job) {
		Map<String, Set<String>> spellings = new HashMap<>();
		for (TechTerm term : techTermRepository.findAll()) {
			addSpellings(spellings, term.canonical(), term.aliases());
		}
		if (job != null) {
			for (String keyword : job.requiredKeywords()) {
				addSpellings(spellings, keyword, job.keywordEquivalents().getOrDefault(keyword, List.of()));
			}
		}
		Map<String, List<Pattern>> patterns = new HashMap<>();
		spellings.forEach((canonical, forms) -> patterns.put(canonical, forms.stream().map(NormalizedText::wordPattern).toList()));
		return new KnownTermVocabulary(patterns);
	}

	private void addSpellings(Map<String, Set<String>> spellings, String canonical, List<String> alternatives) {
		String key = NormalizedText.of(canonical);
		if (key.isBlank()) {
			return;
		}
		List<String> forms = new ArrayList<>();
		forms.add(key);
		alternatives.stream().map(NormalizedText::of).filter(form -> !form.isBlank()).forEach(forms::add);
		spellings.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).addAll(forms);
	}
}
