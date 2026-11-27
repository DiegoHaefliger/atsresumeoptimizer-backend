package com.diegohaefliger.atsresumeoptimizer.diagnostics.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticSection;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.ReadinessCheck;
import java.time.Instant;
import java.util.List;

record DiagnosticReportResponse(Instant generatedAt, List<ReadinessCheck> checks, String verdict,
		List<DiagnosticSection> sections, String text,
		boolean logFileAvailable, boolean logsFolderOpenable) {
}
