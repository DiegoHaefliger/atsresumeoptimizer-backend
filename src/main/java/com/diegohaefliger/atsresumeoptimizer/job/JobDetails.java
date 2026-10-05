package com.diegohaefliger.atsresumeoptimizer.job;

import java.math.BigDecimal;
import java.util.List;

public record JobDetails(
		String company,
		String sourceUrl,
		WorkModel workModel,
		String interviewUrl,
		BigDecimal salary,
		List<String> benefits,
		String title,
		String seniority,
		ContractType contractType) {

	public static final JobDetails NONE = new JobDetails(null, null, null, null, null, List.of(), null, null, null);

	public JobDetails {
		company = blankToNull(company);
		sourceUrl = blankToNull(sourceUrl);
		interviewUrl = blankToNull(interviewUrl);
		title = blankToNull(title);
		seniority = blankToNull(seniority);
		benefits = benefits == null
				? List.of()
				: benefits.stream().filter(benefit -> benefit != null && !benefit.isBlank()).map(String::strip).distinct().toList();
	}

	private static String blankToNull(String text) {
		return text == null || text.isBlank() ? null : text.strip();
	}
}
