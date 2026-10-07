package com.diegohaefliger.atsresumeoptimizer.selection.domain;

import com.diegohaefliger.atsresumeoptimizer.selection.ScheduleStatus;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import java.time.Instant;
import java.util.UUID;

public record SelectionSchedule(
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
