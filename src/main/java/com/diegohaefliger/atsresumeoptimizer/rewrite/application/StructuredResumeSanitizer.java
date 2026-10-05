package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ComposedResumeWithoutNameException;
import java.util.List;
import org.springframework.util.StringUtils;

final class StructuredResumeSanitizer {

	private StructuredResumeSanitizer() {
	}

	static StructuredResume sanitizeNamed(StructuredResume content) {
		StructuredResume sanitized = sanitizeGrammar(content);
		if (!StringUtils.hasText(sanitized.name())) {
			throw new ComposedResumeWithoutNameException();
		}
		return sanitized;
	}

	static StructuredResume sanitizeGrammar(StructuredResume content) {
		List<ResumeSection> sections = content.sections().stream().map(StructuredResumeSanitizer::sanitizeSection).toList();
		return new StructuredResume(GrammarSanitizer.sanitize(content.name()), GrammarSanitizer.sanitize(content.headline()),
				sections, content.removedSkills());
	}

	private static ResumeSection sanitizeSection(ResumeSection section) {
		List<KeyValueLine> keyValues =
				section.keyValues().stream()
						.map(line -> new KeyValueLine(GrammarSanitizer.sanitize(line.label()), GrammarSanitizer.sanitize(line.value())))
						.toList();
		List<List<TextSpan>> richLines = section.richLines().stream().map(StructuredResumeSanitizer::sanitizeSpans).toList();
		List<ResumeEntry> entries = section.entries().stream().map(StructuredResumeSanitizer::sanitizeEntry).toList();
		return new ResumeSection(section.title(), section.semanticType(), section.kind(),
				GrammarSanitizer.sanitize(section.paragraph()), keyValues, richLines, entries);
	}

	private static ResumeEntry sanitizeEntry(ResumeEntry entry) {
		List<List<TextSpan>> bullets = entry.bullets().stream().map(StructuredResumeSanitizer::sanitizeSpans).toList();
		return new ResumeEntry(GrammarSanitizer.sanitize(entry.heading()), GrammarSanitizer.sanitize(entry.period()),
				GrammarSanitizer.sanitize(entry.subheading()), GrammarSanitizer.sanitize(entry.context()), bullets,
				GrammarSanitizer.sanitize(entry.technologies()),
				entry.results().stream().map(StructuredResumeSanitizer::sanitizeSpans).toList());
	}

	private static List<TextSpan> sanitizeSpans(List<TextSpan> spans) {
		return spans.stream().map(span -> span.withText(GrammarSanitizer.sanitize(span.text()))).toList();
	}
}
