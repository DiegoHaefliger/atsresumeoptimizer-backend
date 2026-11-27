package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** O layout separa empresa e local pelo "|"; sem ele o local sai grudado no nome da empresa, em caixa alta. */
final class EntrySubheadingSplitter {

	private static final String LOCATION_SEPARATOR = "|";
	private static final Pattern COMPANY_THEN_LOCATION = Pattern.compile(
			"^(?<company>.+?)\\s*(?:,|\\s[-–—]\\s|[–—])\\s*"
					+ "(?<location>[^,|–—]+?\\s*[/,–-]\\s*"
					+ "(?:AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO))$");

	private EntrySubheadingSplitter() {
	}

	static StructuredResume apply(StructuredResume content) {
		return content.withSections(content.sections().stream().map(EntrySubheadingSplitter::split).toList());
	}

	private static ResumeSection split(ResumeSection section) {
		if (!section.semanticType().holdsBullets()) {
			return section;
		}
		return section.withEntries(section.entries().stream().map(EntrySubheadingSplitter::split).toList());
	}

	private static ResumeEntry split(ResumeEntry entry) {
		String subheading = entry.subheading();
		if (subheading == null || subheading.contains(LOCATION_SEPARATOR)) {
			return entry;
		}
		Matcher matcher = COMPANY_THEN_LOCATION.matcher(subheading.strip());
		if (!matcher.matches()) {
			return entry;
		}
		String split = matcher.group("company").strip() + " " + LOCATION_SEPARATOR + " " + matcher.group("location").strip();
		return new ResumeEntry(entry.heading(), entry.period(), split, entry.context(), entry.bullets(),
				entry.technologies(), entry.results());
	}
}
