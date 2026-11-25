package com.diegohaefliger.atsresumeoptimizer.diagnostics.domain;

import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticSection;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public record DiagnosticReport(Instant generatedAt, List<DiagnosticSection> sections, boolean logFileAvailable,
		boolean logsFolderOpenable) {

	public DiagnosticReport {
		sections = List.copyOf(sections);
	}

	public String asText() {
		String body = sections.stream()
				.map(section -> "## " + section.title() + System.lineSeparator() + section.entries().stream()
						.map(entry -> "- " + entry.label() + ": " + entry.value())
						.collect(Collectors.joining(System.lineSeparator())))
				.collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
		return "Diagnóstico ATS Resume Optimizer (" + generatedAt + ")" + System.lineSeparator() + System.lineSeparator() + body;
	}
}
