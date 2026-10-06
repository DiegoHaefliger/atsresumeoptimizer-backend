package com.diegohaefliger.atsresumeoptimizer.selection.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SelectionProcessData(
		String company,
		String jobTitle,
		String jobUrl,
		String processUrl,
		LocalDate appliedOn,
		LocalDate nextStepOn,
		String contactName,
		String contactEmail,
		BigDecimal salary,
		String notes) {

	public SelectionProcessData {
		company = blankToNull(company);
		jobTitle = blankToNull(jobTitle);
		jobUrl = blankToNull(jobUrl);
		processUrl = blankToNull(processUrl);
		contactName = blankToNull(contactName);
		contactEmail = blankToNull(contactEmail);
		notes = blankToNull(notes);
	}

	private static String blankToNull(String text) {
		return text == null || text.isBlank() ? null : text.strip();
	}
}
