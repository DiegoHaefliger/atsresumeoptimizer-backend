package com.diegohaefliger.atsresumeoptimizer.job.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.HttpUrl;
import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

record CreateJobRequest(
		@NotBlank String text,
		@Size(max = 255) String company,
		@Size(max = 1000) @HttpUrl String sourceUrl,
		WorkModel workModel,
		@Size(max = 1000) @HttpUrl String interviewUrl,
		@PositiveOrZero BigDecimal salary,
		@Size(max = 100) List<@Size(max = 500) String> benefits,
		@Size(max = 255) String title,
		@Size(max = 50) String seniority,
		ContractType contractType) {
}
