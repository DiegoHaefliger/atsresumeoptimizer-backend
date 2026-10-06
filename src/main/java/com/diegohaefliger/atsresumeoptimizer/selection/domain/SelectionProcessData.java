package com.diegohaefliger.atsresumeoptimizer.selection.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SelectionProcessData(
		UUID jobPostingId,
		String processUrl,
		LocalDate appliedOn,
		LocalDate nextStepOn,
		String contactName,
		String contactEmail,
		BigDecimal salary,
		String notes) {

	public SelectionProcessData {
		processUrl = blankToNull(processUrl);
		contactName = blankToNull(contactName);
		contactEmail = blankToNull(contactEmail);
		notes = blankToNull(notes);
	}

	private static String blankToNull(String text) {
		return text == null || text.isBlank() ? null : text.strip();
	}
}
