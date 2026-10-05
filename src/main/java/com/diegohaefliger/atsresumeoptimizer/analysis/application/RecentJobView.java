package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecentJobView(
		UUID id,
		String title,
		String targetRole,
		String jobDescription,
		Instant lastUsedAt,
		Instant registeredAt,
		String company,
		String sourceUrl,
		WorkModel workModel,
		String interviewUrl,
		BigDecimal salary,
		List<String> benefits,
		String seniority,
		ContractType contractType,
		Integer preferenceScore) {
}
