package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.selection.ScheduleStatus;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import java.time.Instant;
import java.util.UUID;

record CalendarEventResponse(
		UUID scheduleId,
		UUID processId,
		String company,
		String jobTitle,
		SelectionStage stage,
		ScheduleStatus status,
		Instant scheduledAt,
		Integer durationMinutes,
		String location,
		String notes) {
}
