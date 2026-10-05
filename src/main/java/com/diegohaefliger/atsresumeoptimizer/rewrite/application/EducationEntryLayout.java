package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import org.springframework.util.StringUtils;

/** Formação sai numa linha só: "Curso — Instituição | Período". */
final class EducationEntryLayout {

	static final String COURSE_INSTITUTION_SEPARATOR = " — ";
	static final String PERIOD_SEPARATOR = " | ";

	private EducationEntryLayout() {
	}

	static String headline(ResumeEntry entry) {
		String period = period(entry);
		return period.isEmpty() ? title(entry) : title(entry) + PERIOD_SEPARATOR + period;
	}

	static String title(ResumeEntry entry) {
		StringBuilder line = new StringBuilder(entry.heading() == null ? "" : entry.heading().strip());
		if (StringUtils.hasText(entry.subheading())) {
			line.append(line.isEmpty() ? "" : COURSE_INSTITUTION_SEPARATOR).append(entry.subheading().strip());
		}
		return line.toString();
	}

	static String period(ResumeEntry entry) {
		return StringUtils.hasText(entry.period()) ? entry.period().strip().replace(" - ", " – ") : "";
	}
}
