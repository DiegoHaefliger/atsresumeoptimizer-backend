package com.diegohaefliger.atsresumeoptimizer.jobnote.domain;

import java.time.Instant;
import java.util.UUID;

public record JobNote(UUID jobPostingId, String content, Instant updatedAt) {

	public static JobNote empty(UUID jobPostingId) {
		return new JobNote(jobPostingId, "", null);
	}
}
