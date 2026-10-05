package com.diegohaefliger.atsresumeoptimizer.analysis.adapter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

record AnalyzeExistingVersionRequest(
		@NotNull UUID resumeVersionId,
		UUID jobId,
		String jobDescription,
		String targetRole,
		@Valid JobDetailsParams jobDetails) {
}
