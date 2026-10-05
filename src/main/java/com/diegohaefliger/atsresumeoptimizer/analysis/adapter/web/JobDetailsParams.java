package com.diegohaefliger.atsresumeoptimizer.analysis.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.HttpUrl;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import jakarta.validation.constraints.Size;

record JobDetailsParams(
		@Size(max = 255) String jobCompany,
		@Size(max = 1000) @HttpUrl String jobUrl,
		WorkModel jobWorkModel) {
}
