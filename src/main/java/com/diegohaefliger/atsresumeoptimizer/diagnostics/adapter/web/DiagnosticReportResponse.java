package com.diegohaefliger.atsresumeoptimizer.diagnostics.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticSection;
import java.time.Instant;
import java.util.List;

record DiagnosticReportResponse(Instant generatedAt, List<DiagnosticSection> sections, String text,
		boolean logFileAvailable, boolean logsFolderOpenable) {
}
