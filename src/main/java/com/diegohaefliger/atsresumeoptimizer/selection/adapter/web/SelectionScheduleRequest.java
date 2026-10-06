package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

record SelectionScheduleRequest(
		SelectionStage stage,
		@NotNull Instant scheduledAt,
		@Positive @Max(1440) Integer durationMinutes,
		@Size(max = 1000) String location,
		@Size(max = 2000) String notes) {
}
