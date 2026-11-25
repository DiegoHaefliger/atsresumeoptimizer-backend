package com.diegohaefliger.atsresumeoptimizer.diagnostics;

public record DiagnosticEntry(String label, String value, boolean personal) {

	public DiagnosticEntry(String label, String value) {
		this(label, value, false);
	}

	public static DiagnosticEntry personal(String label, String value) {
		return new DiagnosticEntry(label, value, true);
	}
}
