package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.selection.CalendarEvent;
import com.diegohaefliger.atsresumeoptimizer.selection.ScheduleStatus;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class IcsFeedWriter {

	private static final String CRLF = "\r\n";
	private static final int MAX_LINE_OCTETS = 75;
	private static final Duration DEFAULT_DURATION = Duration.ofHours(1);
	private static final DateTimeFormatter UTC_STAMP =
			DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

	String write(List<CalendarEvent> events, Instant stamp) {
		StringBuilder out = new StringBuilder();
		line(out, "BEGIN:VCALENDAR");
		line(out, "VERSION:2.0");
		line(out, "PRODID:-//ATS Resume Optimizer//Agenda//PT-BR");
		line(out, "CALSCALE:GREGORIAN");
		line(out, "X-WR-CALNAME:Processos seletivos");
		events.forEach(event -> writeEvent(out, event, stamp));
		line(out, "END:VCALENDAR");
		return out.toString();
	}

	private void writeEvent(StringBuilder out, CalendarEvent event, Instant stamp) {
		Duration duration =
				event.durationMinutes() == null ? DEFAULT_DURATION : Duration.ofMinutes(event.durationMinutes());
		line(out, "BEGIN:VEVENT");
		line(out, "UID:" + event.scheduleId() + "@atsresumeoptimizer");
		line(out, "DTSTAMP:" + UTC_STAMP.format(stamp));
		line(out, "DTSTART:" + UTC_STAMP.format(event.scheduledAt()));
		line(out, "DTEND:" + UTC_STAMP.format(event.scheduledAt().plus(duration)));
		line(out, "SUMMARY:" + escape(summary(event)));
		if (event.location() != null) {
			line(out, "LOCATION:" + escape(event.location()));
		}
		if (event.notes() != null) {
			line(out, "DESCRIPTION:" + escape(event.notes()));
		}
		line(out, "STATUS:" + (isActive(event) ? "CONFIRMED" : "CANCELLED"));
		line(out, "END:VEVENT");
	}

	private static boolean isActive(CalendarEvent event) {
		return event.status() == ScheduleStatus.SCHEDULED || event.status() == ScheduleStatus.DONE;
	}

	private static String summary(CalendarEvent event) {
		String subject = List.of(nullToEmpty(event.company()), nullToEmpty(event.jobTitle())).stream()
				.filter(part -> !part.isBlank())
				.collect(Collectors.joining(" - "));
		String label = event.stage().label();
		return subject.isEmpty() ? label : label + ": " + subject;
	}

	private static String nullToEmpty(String text) {
		return text == null ? "" : text;
	}

	private static String escape(String text) {
		return text.replace("\\", "\\\\")
				.replace(";", "\\;")
				.replace(",", "\\,")
				.replace("\r\n", "\\n")
				.replace("\n", "\\n");
	}

	private static void line(StringBuilder out, String content) {
		StringBuilder current = new StringBuilder();
		int octets = 0;
		for (int index = 0; index < content.length(); ) {
			int codePoint = content.codePointAt(index);
			String character = new String(Character.toChars(codePoint));
			int size = character.getBytes(StandardCharsets.UTF_8).length;
			if (octets + size > MAX_LINE_OCTETS) {
				out.append(current).append(CRLF);
				current = new StringBuilder(" ");
				octets = 1;
			}
			current.append(character);
			octets += size;
			index += Character.charCount(codePoint);
		}
		out.append(current).append(CRLF);
	}
}
