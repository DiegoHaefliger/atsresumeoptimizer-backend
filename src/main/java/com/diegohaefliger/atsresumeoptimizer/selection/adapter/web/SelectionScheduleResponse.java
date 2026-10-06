package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.selection.ScheduleStatus;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import java.time.Instant;
import java.util.UUID;

record SelectionScheduleResponse(
		UUID id,
		UUID processId,
		SelectionStage stage,
		Instant scheduledAt,
		Integer durationMinutes,
		String location,
		String notes,
		ScheduleStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
