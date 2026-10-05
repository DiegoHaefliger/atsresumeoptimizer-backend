package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reconhece o layout que o próprio sistema gera: título + período, subtítulo, contexto, bullets e "Tecnologias:". */
final class EntrySectionParser {

	private static final String MONTH = "(?:(?:jan|fev|feb|mar|abr|apr|mai|may|jun|jul|ago|aug|set|sep|out|oct|nov|dez|dec)\\p{L}*\\.?|\\d{1,2})\\s*(?:de\\s*|/\\s*)?";
	private static final String DATE = "(?:" + MONTH + ")?\\d{4}";
	private static final String OPEN_END = "presente|atual|atualmente|present|current|hoje|now";
	private static final Pattern HEADING_WITH_PERIOD = Pattern.compile(
			"^(?<heading>.*?)[\\s|,(\\-–—]*(?<period>" + DATE + "\\s*(?:-|–|—|a|até|to)\\s*(?:" + DATE + "|" + OPEN_END
					+ "))[\\s)]*$",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern RESULTS_LABEL = Pattern.compile("^resultados\\s*:?$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern COMPANY_ROLE_LOCATION = Pattern.compile("^(?<company>.+?)\\s+—\\s+(?<role>[^|]+?)(?:\\s*\\|\\s*(?<location>.+))?$");
	private static final Pattern COURSE_INSTITUTION = Pattern.compile("^(?<course>.+?)\\s+—\\s+(?<institution>.+)$");
	private static final Pattern TECHNOLOGIES = Pattern.compile("^(?:tecnologias|technologies)\\s*:\\s*(?<list>.*)$",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private EntrySectionParser() {
	}

	static Optional<List<ResumeEntry>> parse(List<ResumeTextLine> lines, ResumeSectionSemanticType type) {
		List<ResumeEntry> entries = new ArrayList<>();
		EntryBuilder current = null;
		for (int index = 0; index < lines.size(); index++) {
			ResumeTextLine line = lines.get(index);
			Optional<Heading> heading = headingAt(lines, index);
			if (heading.isPresent()) {
				if (current != null) {
					entries.add(current.build());
				}
				current = new EntryBuilder(heading.get(), type);
				index += heading.get().linesUsed() - 1;
			} else if (current != null) {
				current.add(line);
			} else {
				return Optional.empty();
			}
		}
		if (current != null) {
			entries.add(current.build());
		}
		return entries.isEmpty() ? Optional.empty() : Optional.of(entries);
	}

	private static Optional<Heading> headingAt(List<ResumeTextLine> lines, int index) {
		ResumeTextLine line = lines.get(index);
		if (line.bullet() || TECHNOLOGIES.matcher(line.text()).matches()) {
			return Optional.empty();
		}
		Matcher inline = HEADING_WITH_PERIOD.matcher(line.text());
		if (inline.matches()) {
			String text = inline.group("heading").strip();
			if (!text.isEmpty()) {
				return Optional.of(new Heading(text, inline.group("period").strip(), 1));
			}
			return Optional.empty();
		}
		if (index + 1 < lines.size() && !lines.get(index + 1).bullet()) {
			Matcher standalone = HEADING_WITH_PERIOD.matcher(lines.get(index + 1).text());
			if (standalone.matches() && standalone.group("heading").isBlank()) {
				return Optional.of(new Heading(line.text(), standalone.group("period").strip(), 2));
			}
		}
		return Optional.empty();
	}

	private record Heading(String text, String period, int linesUsed) {
	}

	private static final class EntryBuilder {

		private final Heading heading;
		private final ResumeSectionSemanticType type;
		private final List<String> details = new ArrayList<>();
		private final List<List<TextSpan>> bullets = new ArrayList<>();
		private final List<List<TextSpan>> results = new ArrayList<>();
		private boolean readingResults;
		private String technologies;

		EntryBuilder(Heading heading, ResumeSectionSemanticType type) {
			this.heading = heading;
			this.type = type;
		}

		void add(ResumeTextLine line) {
			Matcher tech = TECHNOLOGIES.matcher(line.text());
			if (tech.matches()) {
				technologies = tech.group("list").strip();
			} else if (RESULTS_LABEL.matcher(line.text()).matches()) {
				readingResults = true;
			} else if (readingResults) {
				results.add(List.of(new TextSpan(line.text(), false)));
			} else if (line.bullet() || !bullets.isEmpty()) {
				bullets.add(List.of(new TextSpan(line.text(), false)));
			} else {
				details.add(line.text());
			}
		}

		ResumeEntry build() {
			if (type == ResumeSectionSemanticType.EDUCATION) {
				Matcher course = COURSE_INSTITUTION.matcher(heading.text());
				if (course.matches()) {
					return new ResumeEntry(course.group("course").strip(), heading.period(), course.group("institution").strip(),
							null, bullets, technologies, results);
				}
			}
			Matcher headline = type == ResumeSectionSemanticType.EXPERIENCE ? COMPANY_ROLE_LOCATION.matcher(heading.text()) : null;
			if (headline != null && headline.matches()) {
				String company = headline.group("company").strip();
				String location = headline.group("location");
				String subheading = location == null ? company : company + " | " + location.strip();
				return new ResumeEntry(headline.group("role").strip(), heading.period(), subheading, null, bullets,
						technologies, results);
			}
			String subheading = details.isEmpty() ? null : details.getFirst();
			String context = details.size() > 1 ? String.join("\n", details.subList(1, details.size())) : null;
			return new ResumeEntry(heading.text(), heading.period(), subheading, context, bullets, technologies, results);
		}
	}
}
