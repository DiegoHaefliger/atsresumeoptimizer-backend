package com.diegohaefliger.atsresumeoptimizer.selection;

import java.time.Instant;
import java.util.UUID;

public record CalendarEvent(
		UUID scheduleId,
		UUID processId,
		String company,
		String jobTitle,
		SelectionStage stage,
		ScheduleStatus status,
		Instant scheduledAt,
		Integer durationMinutes,
		String location,
		String notes,
		RecruiterContact recruiter) {
}
