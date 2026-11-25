package com.diegohaefliger.atsresumeoptimizer.diagnostics.application;

import com.diegohaefliger.atsresumeoptimizer.diagnostics.domain.ClientError;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.domain.DiagnosticBundle;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.domain.DiagnosticReport;

public interface DiagnosticsService {

	DiagnosticReport report();

	DiagnosticBundle bundle();

	String recordClientError(ClientError error);

	void openLogsFolder();
}
