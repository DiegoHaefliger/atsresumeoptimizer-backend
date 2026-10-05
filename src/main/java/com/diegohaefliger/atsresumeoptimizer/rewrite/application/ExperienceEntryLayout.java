package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/** Experiência sai como "EMPRESA — Cargo | Local", período na linha seguinte e tudo o mais em bullets. */
final class ExperienceEntryLayout {

	private static final Pattern COMPANY_LOCATION_SEPARATOR = Pattern.compile("\\s*\\|\\s*");
	private static final String COMPANY_ROLE_SEPARATOR = " — ";
	private static final String LOCATION_SEPARATOR = " | ";

	private ExperienceEntryLayout() {
	}

	static String headline(ResumeEntry entry) {
		String location = location(entry);
		return location.isEmpty() ? title(entry) : title(entry) + LOCATION_SEPARATOR + location;
	}

	static String title(ResumeEntry entry) {
		String company = company(entry).toUpperCase(Locale.ROOT);
		String role = entry.heading() == null ? "" : entry.heading().strip();
		if (role.isEmpty()) {
			return company;
		}
		return company.isEmpty() ? role : company + COMPANY_ROLE_SEPARATOR + role;
	}

	static String location(ResumeEntry entry) {
		String[] parts = subheadingParts(entry);
		return parts.length > 1 ? parts[1].strip() : "";
	}

	private static String company(ResumeEntry entry) {
		String[] parts = subheadingParts(entry);
		return parts.length > 0 ? parts[0].strip() : "";
	}

	private static String[] subheadingParts(ResumeEntry entry) {
		return entry.subheading() == null ? new String[0]
				: COMPANY_LOCATION_SEPARATOR.split(entry.subheading().strip(), 2);
	}

	static String period(ResumeEntry entry) {
		return StringUtils.hasText(entry.period()) ? entry.period().strip().replace(" - ", " – ") : "";
	}

	static List<List<TextSpan>> bulletLines(ResumeEntry entry) {
		List<List<TextSpan>> lines = new ArrayList<>(entry.bullets());
		if (StringUtils.hasText(entry.context())) {
			RichText.fromMarkup(entry.context()).stream().filter(line -> !line.isBlank())
					.forEach(line -> lines.add(line.spans()));
		}
		lines.addAll(entry.results());
		return lines;
	}
}
