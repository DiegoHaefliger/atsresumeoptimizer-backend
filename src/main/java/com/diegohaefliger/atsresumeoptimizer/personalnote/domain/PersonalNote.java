package com.diegohaefliger.atsresumeoptimizer.personalnote.domain;

import java.time.Instant;

public record PersonalNote(String content, Instant updatedAt) {

	public static PersonalNote empty() {
		return new PersonalNote("", null);
	}
}
