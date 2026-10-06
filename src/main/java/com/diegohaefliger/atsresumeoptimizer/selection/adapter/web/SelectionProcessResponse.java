package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

record SelectionProcessResponse(
		UUID id,
		String company,
		String jobTitle,
		String jobUrl,
		String processUrl,
		SelectionStage stage,
		LocalDate appliedOn,
		LocalDate nextStepOn,
		String contactName,
		String contactEmail,
		BigDecimal salary,
		String notes,
		Instant createdAt,
		Instant updatedAt,
		List<StageMovementResponse> history) {
}
