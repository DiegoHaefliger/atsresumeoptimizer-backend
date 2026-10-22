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
		String rawText,
		String companyInfo,
		List<String> benefitsMarkup) {

	public JobOffer {
		company = absentIfLiteralNull(company);
		location = absentIfLiteralNull(location);
		seniority = absentIfLiteralNull(seniority);
		benefits = benefits == null ? List.of() : List.copyOf(benefits);
		benefitsMarkup = benefitsMarkup == null ? benefits : List.copyOf(benefitsMarkup);
		benefits = benefits.stream().map(InlineMarkup::strip).toList();
	}

	public JobOffer(UUID jobPostingId, Long code, String title, String company, String sourceUrl, String interviewUrl,
			WorkModel workModel, ContractType contractType, BigDecimal salaryMin, BigDecimal salaryMax, List<String> benefits,
			String location, String seniority, String rawText) {
		this(jobPostingId, code, title, company, sourceUrl, interviewUrl, workModel, contractType, salaryMin, salaryMax, benefits,
				location, seniority, rawText, null, null);
	}

	public JobOffer(UUID jobPostingId, Long code, String title, String company, String sourceUrl, String interviewUrl,
			WorkModel workModel, ContractType contractType, BigDecimal salaryMin, BigDecimal salaryMax, List<String> benefits,
			String location, String seniority, String rawText, String companyInfo) {
		this(jobPostingId, code, title, company, sourceUrl, interviewUrl, workModel, contractType, salaryMin, salaryMax, benefits,
				location, seniority, rawText, companyInfo, null);
	}

	private static String absentIfLiteralNull(String value) {
		return value == null || value.isBlank() || "null".equalsIgnoreCase(value.strip()) ? null : value;
	}
}
