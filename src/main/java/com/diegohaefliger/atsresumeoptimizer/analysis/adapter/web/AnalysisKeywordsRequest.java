package com.diegohaefliger.atsresumeoptimizer.analysis.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

record AnalysisKeywordsRequest(@NotNull @Size(max = 100) List<@NotBlank @Size(max = 100) String> keywords,
		@NotNull @Size(max = JobStructured.MAX_PRIORITY_COUNT) List<@NotBlank @Size(max = 100) String> selected) {
}
