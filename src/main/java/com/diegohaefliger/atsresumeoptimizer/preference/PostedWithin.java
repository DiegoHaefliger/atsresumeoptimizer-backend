package com.diegohaefliger.atsresumeoptimizer.preference;

import java.time.Duration;

public enum PostedWithin {
	ANY(null),
	PAST_HOUR(Duration.ofHours(1)),
	PAST_2_HOURS(Duration.ofHours(2)),
	PAST_6_HOURS(Duration.ofHours(6)),
	PAST_12_HOURS(Duration.ofHours(12)),
	PAST_24_HOURS(Duration.ofHours(24)),
	PAST_WEEK(Duration.ofDays(7)),
	PAST_MONTH(Duration.ofDays(30));

	private final Duration window;

	PostedWithin(Duration window) {
		this.window = window;
	}

	public Duration window() {
		return window;
	}
}
