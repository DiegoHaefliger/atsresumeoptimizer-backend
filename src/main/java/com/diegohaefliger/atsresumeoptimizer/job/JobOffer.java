package com.diegohaefliger.atsresumeoptimizer.job;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record JobOffer(
		UUID jobPostingId,
		Long code,
		String title,
		String company,
		String sourceUrl,
		String interviewUrl,
		WorkModel workModel,
		ContractType contractType,
		BigDecimal salaryMin,
		BigDecimal salaryMax,
		List<String> benefits,
		String location,
		String seniority,
		String rawText) {

	public JobOffer {
		company = absentIfLiteralNull(company);
		location = absentIfLiteralNull(location);
		seniority = absentIfLiteralNull(seniority);
		benefits = benefits == null ? List.of() : List.copyOf(benefits);
	}

	private static String absentIfLiteralNull(String value) {
		return value == null || value.isBlank() || "null".equalsIgnoreCase(value.strip()) ? null : value;
	}
}
