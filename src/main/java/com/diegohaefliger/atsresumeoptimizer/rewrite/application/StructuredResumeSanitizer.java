package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ComposedResumeWithoutNameException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class StructuredResumeSanitizer {

	private static final String INSTITUTION_SEPARATOR = " — ";
	private static final String PERIOD_SEPARATOR = " | ";
	private static final Pattern GLUED_WORDS = Pattern.compile("[\\p{Ll}\\d](?=\\p{Lu})");

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
		boolean titled = section.semanticType() == ResumeSectionSemanticType.EDUCATION
				|| section.semanticType() == ResumeSectionSemanticType.CERTIFICATIONS;
		List<List<TextSpan>> richLines = section.richLines().stream().map(StructuredResumeSanitizer::sanitizeSpans)
				.map(line -> titled ? separateGluedWords(separateTitleFromInstitution(line)) : line).toList();
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

	private static List<TextSpan> separateTitleFromInstitution(List<TextSpan> line) {
		if (line.size() < 2 || !line.get(0).bold() || line.get(1).bold()) {
			return line;
		}
		String title = line.get(0).text();
		String rest = line.get(1).text();
		boolean glued = StringUtils.hasText(title) && !title.endsWith(" ") && StringUtils.hasText(rest)
				&& Character.isLetterOrDigit(rest.charAt(0));
		if (!glued) {
			return line;
		}
		List<TextSpan> separated = new ArrayList<>(line);
		separated.set(0, line.get(0).withText(title + INSTITUTION_SEPARATOR));
		return separated;
	}

	private static List<TextSpan> separateGluedWords(List<TextSpan> line) {
		String text = TextSpan.plainText(line);
		int periodAt = text.indexOf(PERIOD_SEPARATOR);
		String head = periodAt < 0 ? text : text.substring(0, periodAt);
		if (head.contains(INSTITUTION_SEPARATOR)) {
			return line;
		}
		Matcher glue = GLUED_WORDS.matcher(head);
		int boundary = -1;
		while (glue.find()) {
			boundary = glue.end();
		}
		return boundary < 0 ? line : insertAt(line, boundary, INSTITUTION_SEPARATOR);
	}

	private static List<TextSpan> insertAt(List<TextSpan> line, int offset, String insertion) {
		List<TextSpan> result = new ArrayList<>();
		int start = 0;
		for (TextSpan span : line) {
			int end = start + span.text().length();
			if (offset > start && offset <= end) {
				int local = offset - start;
				result.add(span.withText(span.text().substring(0, local) + insertion + span.text().substring(local)));
				offset = Integer.MAX_VALUE;
			} else {
				result.add(span);
			}
			start = end;
		}
		return result;
	}

	private static List<TextSpan> sanitizeSpans(List<TextSpan> spans) {
		return spans.stream().map(span -> span.withText(GrammarSanitizer.sanitize(span.text()))).toList();
	}
}
