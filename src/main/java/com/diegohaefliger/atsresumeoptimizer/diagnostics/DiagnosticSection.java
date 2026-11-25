package com.diegohaefliger.atsresumeoptimizer.diagnostics;

import java.util.List;

public record DiagnosticSection(String title, List<DiagnosticEntry> entries) {

	public DiagnosticSection {
		entries = List.copyOf(entries);
	}
}
