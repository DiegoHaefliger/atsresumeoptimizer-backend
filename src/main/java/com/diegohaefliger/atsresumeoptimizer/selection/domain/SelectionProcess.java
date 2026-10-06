package com.diegohaefliger.atsresumeoptimizer.selection.domain;

import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SelectionProcess(
		UUID id,
		UUID jobPostingId,
		Long jobCode,
		String company,
		String jobTitle,
		String jobUrl,
		String processUrl,
		SelectionStage stage,
		LocalDate appliedOn,
		LocalDate nextStepOn,
		String contactName,
		String contactEmail,
		String contactPhone,
		BigDecimal salary,
		String notes,
		Instant createdAt,
		Instant updatedAt,
		List<StageMovement> history) {

	public SelectionProcess {
		history = history == null ? List.of() : List.copyOf(history);
	}
}
