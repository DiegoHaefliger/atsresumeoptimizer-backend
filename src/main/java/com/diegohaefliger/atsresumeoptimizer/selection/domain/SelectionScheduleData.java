package com.diegohaefliger.atsresumeoptimizer.selection.domain;

import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import java.time.Instant;

public record SelectionScheduleData(
		SelectionStage stage, Instant scheduledAt, Integer durationMinutes, String location, String notes) {

	public SelectionScheduleData {
		location = blankToNull(location);
		notes = blankToNull(notes);
	}

	private static String blankToNull(String text) {
		return text == null || text.isBlank() ? null : text.strip();
	}
}
