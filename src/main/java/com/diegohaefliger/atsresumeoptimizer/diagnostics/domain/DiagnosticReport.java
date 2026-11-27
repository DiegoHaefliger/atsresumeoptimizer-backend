package com.diegohaefliger.atsresumeoptimizer.diagnostics.domain;

import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticSection;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.ReadinessCheck;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.ReadinessStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record DiagnosticReport(Instant generatedAt, List<ReadinessCheck> checks, List<DiagnosticSection> sections,
		boolean logFileAvailable, boolean logsFolderOpenable) {

	private static final String NEW_LINE = System.lineSeparator();
	private static final Map<ReadinessStatus, String> STATUS_TAGS =
			Map.of(ReadinessStatus.OK, "[OK]", ReadinessStatus.WARNING, "[ATENÇÃO]", ReadinessStatus.FAILURE, "[FALHA]");

	public DiagnosticReport {
		checks = List.copyOf(checks);
		sections = List.copyOf(sections);
	}

	public String asText() {
		String body = sections.stream()
				.map(section -> "## " + section.title() + NEW_LINE + section.entries().stream()
						.map(entry -> "- " + entry.label() + ": " + entry.value())
						.collect(Collectors.joining(NEW_LINE)))
				.collect(Collectors.joining(NEW_LINE + NEW_LINE));
		return "Diagnóstico ATS Resume Optimizer (" + generatedAt + ")" + NEW_LINE + NEW_LINE + checksText() + body;
	}

	public String verdict() {
		long failures = count(ReadinessStatus.FAILURE);
		long warnings = count(ReadinessStatus.WARNING);
		if (failures == 0 && warnings == 0) {
			return "Tudo pronto pra usar.";
		}
		String failureText = failures == 1 ? "1 falha" : failures + " falhas";
		String warningText = warnings == 1 ? "1 alerta" : warnings + " alertas";
		return failures == 0 ? warningText + "." : warnings == 0 ? failureText + "." : failureText + " e " + warningText + ".";
	}

	private long count(ReadinessStatus status) {
		return checks.stream().filter(check -> check.status() == status).count();
	}

	private String checksText() {
		if (checks.isEmpty()) {
			return "";
		}
		String lines = checks.stream().map(DiagnosticReport::checkLine).collect(Collectors.joining(NEW_LINE));
		return "## Verificação" + NEW_LINE + "Resultado: " + verdict() + NEW_LINE + lines + NEW_LINE + NEW_LINE;
	}

	private static String checkLine(ReadinessCheck check) {
		String line = "- " + STATUS_TAGS.get(check.status()) + " " + check.name() + ": " + check.detail();
		return check.hint() == null ? line : line + NEW_LINE + "  Como resolver: " + check.hint();
	}
}
