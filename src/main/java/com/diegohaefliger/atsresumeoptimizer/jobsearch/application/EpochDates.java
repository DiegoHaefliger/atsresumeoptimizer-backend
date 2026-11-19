package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

final class EpochDates {

	private EpochDates() {
	}

	static LocalDate fromEpochSeconds(long seconds) {
		return seconds <= 0 ? null : Instant.ofEpochSecond(seconds).atZone(ZoneOffset.UTC).toLocalDate();
	}

	static LocalDate fromIso(String value) {
		try {
			return value == null || value.isBlank() ? null : OffsetDateTime.parse(value).toLocalDate();
		} catch (DateTimeParseException exception) {
			return null;
		}
	}
}
