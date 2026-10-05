package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.JobKeywordMatcher;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.KeywordMatch;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.SpellingVariant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class KeywordMatcher implements JobKeywordMatcher {

	private final TechTermRepository techTermRepository;

	KeywordMatcher(TechTermRepository techTermRepository) {
		this.techTermRepository = techTermRepository;
	}

	@Override
	public List<KeywordMatch> match(ParsingResult parsing, JobStructured job) {
		if (job == null || job.requiredKeywords().isEmpty()) {
			return List.of();
		}
		String haystack = NormalizedText.of(parsing.document().rawText());
		Map<String, List<String>> aliasesByCanonical = techTermRepository.findAll().stream()
				.collect(Collectors.toMap(term -> NormalizedText.of(term.canonical()), this::normalizedAliases));
		return job.requiredKeywords().stream()
				.map(term -> matchKeyword(term, haystack, aliasesByCanonical, job, parsing.sections()))
				.toList();
	}

	private KeywordMatch matchKeyword(
			String term, String haystack, Map<String, List<String>> aliasesByCanonical, JobStructured job,
			List<Section> sections) {
		String normalizedTerm = NormalizedText.of(term);
		boolean foundExact = NormalizedText.containsWord(haystack, normalizedTerm)
				|| aliasesByCanonical.getOrDefault(normalizedTerm, List.of()).stream()
						.anyMatch(alias -> NormalizedText.containsWord(haystack, alias))
				|| containsWithLooseSeparators(haystack, normalizedTerm)
				|| SpellingVariant.containedIn(haystack, normalizedTerm);

		List<String> equivalents = job.keywordEquivalents().getOrDefault(term, List.of());
		boolean foundSemantic = !foundExact
				&& equivalents.stream().anyMatch(equivalent -> NormalizedText.containsWord(haystack, NormalizedText.of(equivalent)));

		int occurrences = NormalizedText.countWord(haystack, normalizedTerm);
		List<String> foundInSections = sections.stream()
				.filter(section -> NormalizedText.containsWord(NormalizedText.of(section.content()), normalizedTerm))
				.map(Section::title)
				.toList();

		return new KeywordMatch(term, null, true, foundExact, foundSemantic, occurrences, foundInSections);
	}

	/** "node.js" casa "nodejs", "ci/cd" casa "ci-cd": separador de palavra composta varia entre currículos. */
	private boolean containsWithLooseSeparators(String haystack, String normalizedTerm) {
		String[] parts = normalizedTerm.split("[\\s./-]+");
		if (parts.length < 2) {
			return false;
		}
		String pattern = Arrays.stream(parts).map(Pattern::quote).collect(Collectors.joining("[\\s./-]*"));
		return Pattern.compile("\\b" + pattern + "\\b").matcher(haystack).find();
	}

	private List<String> normalizedAliases(TechTerm term) {
		return term.aliases().stream().map(NormalizedText::of).toList();
	}

}
