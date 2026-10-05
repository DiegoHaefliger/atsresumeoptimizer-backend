package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.SpellingVariant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Termo da vaga entre parênteses que só repete a palavra anterior com outra grafia: "microsserviços (microserviços)". */
final class RedundantParentheticalCleaner {

	private static final Pattern PARENTHETICAL = Pattern.compile("\\s*\\(([^()]{1,80})\\)");

	private RedundantParentheticalCleaner() {
	}

	static StructuredResume apply(StructuredResume content) {
		return content.withSections(content.sections().stream()
				.map(section -> section
						.withContent(clean(section.paragraph()),
								section.keyValues().stream().map(line -> new KeyValueLine(line.label(), clean(line.value()))).toList(),
								section.richLines().stream().map(RedundantParentheticalCleaner::cleanSpans).toList())
						.withEntries(section.entries().stream().map(RedundantParentheticalCleaner::cleanEntry).toList()))
				.toList());
	}

	static String clean(String text) {
		if (text == null || text.indexOf('(') < 0) {
			return text;
		}
		Matcher matcher = PARENTHETICAL.matcher(text);
		StringBuilder result = new StringBuilder();
		int last = 0;
		while (matcher.find()) {
			String before = text.substring(0, matcher.start());
			if (repeatsPrecedingWords(before, matcher.group(1))) {
				result.append(text, last, matcher.start());
				last = matcher.end();
			}
		}
		return result.append(text.substring(last)).toString();
	}

	private static boolean repeatsPrecedingWords(String before, String inner) {
		int innerSize = SpellingVariant.words(inner).size();
		List<String> precedingWords = SpellingVariant.words(before);
		if (innerSize == 0 || precedingWords.size() < innerSize) {
			return false;
		}
		String preceding = String.join(" ", precedingWords.subList(precedingWords.size() - innerSize, precedingWords.size()));
		return SpellingVariant.same(preceding, inner);
	}

	private static ResumeEntry cleanEntry(ResumeEntry entry) {
		return new ResumeEntry(entry.heading(), entry.period(), entry.subheading(), clean(entry.context()),
				entry.bullets().stream().map(RedundantParentheticalCleaner::cleanSpans).toList(), clean(entry.technologies()),
				entry.results().stream().map(RedundantParentheticalCleaner::cleanSpans).toList());
	}

	private static List<TextSpan> cleanSpans(List<TextSpan> spans) {
		return spans.stream().map(span -> span.withText(clean(span.text()))).toList();
	}
}
