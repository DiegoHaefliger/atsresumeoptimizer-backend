package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.selection.CalendarEvent;
import com.diegohaefliger.atsresumeoptimizer.selection.ScheduleStatus;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IcsFeedWriterTest {

	private static final Instant STAMP = Instant.parse("2026-10-06T12:00:00Z");
	private static final UUID SCHEDULE_ID = UUID.randomUUID();

	private final IcsFeedWriter writer = new IcsFeedWriter();

	private CalendarEvent event(ScheduleStatus status, String notes) {
		return new CalendarEvent(SCHEDULE_ID, UUID.randomUUID(), "Acme", "Dev Java", SelectionStage.TECHNICAL_INTERVIEW,
				status, Instant.parse("2026-10-08T17:00:00Z"), 30, "https://meet.example/abc", notes);
	}

	@Test
	void writesAValidCalendarWithCrlfLineEndings() {
		String ics = writer.write(List.of(event(ScheduleStatus.SCHEDULED, null)), STAMP);

		assertThat(ics).startsWith("BEGIN:VCALENDAR\r\n").endsWith("END:VCALENDAR\r\n");
		assertThat(ics).contains("UID:" + SCHEDULE_ID + "@atsresumeoptimizer\r\n");
		assertThat(ics).contains("DTSTART:20261008T170000Z\r\n", "DTEND:20261008T173000Z\r\n", "DTSTAMP:20261006T120000Z\r\n");
		assertThat(ics).contains("SUMMARY:Entrevista técnica: Acme - Dev Java\r\n", "STATUS:CONFIRMED\r\n");
		assertThat(ics).doesNotContain("DESCRIPTION");
	}

	@Test
	void marksCanceledAndRescheduledEventsAsCancelled() {
		assertThat(writer.write(List.of(event(ScheduleStatus.CANCELED, null)), STAMP)).contains("STATUS:CANCELLED");
		assertThat(writer.write(List.of(event(ScheduleStatus.RESCHEDULED, null)), STAMP)).contains("STATUS:CANCELLED");
	}

	@Test
	void escapesSpecialCharactersAndFoldsLongLines() {
		String notes = "levar portfólio; revisar, ensaiar\nperguntas " + "x".repeat(120);

		String ics = writer.write(List.of(event(ScheduleStatus.SCHEDULED, notes)), STAMP);

		assertThat(ics).contains("levar portf");
		assertThat(ics).contains("\\;").contains("\\,").contains("\\n");
		assertThat(ics.split("\r\n")).allSatisfy(line -> assertThat(line.getBytes(StandardCharsets.UTF_8).length)
				.isLessThanOrEqualTo(75));
	}
}
