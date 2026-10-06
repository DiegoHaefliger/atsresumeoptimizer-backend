package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import com.diegohaefliger.atsresumeoptimizer.selection.CalendarEvent;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
class GoogleEventPayloads {

	private static final Duration DEFAULT_DURATION = Duration.ofHours(1);
	private static final int MAX_REMINDER_MINUTES = 40_320;
	private static final int MAX_REMINDERS = 5;

	private final GoogleCalendarProperties properties;

	GoogleEventPayloads(GoogleCalendarProperties properties) {
		this.properties = properties;
	}

	Map<String, Object> create(CalendarEvent event, List<Integer> leadMinutes) {
		Duration duration =
				event.durationMinutes() == null ? DEFAULT_DURATION : Duration.ofMinutes(event.durationMinutes());
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("summary", summary(event));
		body.put("description", description(event));
		if (event.location() != null) {
			body.put("location", event.location());
		}
		body.put("start", Map.of("dateTime", event.scheduledAt().toString()));
		body.put("end", Map.of("dateTime", event.scheduledAt().plus(duration).toString()));
		body.put("reminders", reminders(leadMinutes));
		return body;
	}

	private String description(CalendarEvent event) {
		String link = properties.frontendUrl() + "/processes/" + event.processId();
		return Stream.of(event.notes(), "Processo: " + link)
				.filter(part -> part != null && !part.isBlank())
				.reduce((first, second) -> first + "\n\n" + second)
				.orElse(link);
	}

	private static String summary(CalendarEvent event) {
		String subject = Stream.of(event.company(), event.jobTitle())
				.filter(part -> part != null && !part.isBlank())
				.reduce((first, second) -> first + " - " + second)
				.orElse("");
		String label = event.stage().label();
		return subject.isEmpty() ? label : label + ": " + subject;
	}

	private static Map<String, Object> reminders(List<Integer> leadMinutes) {
		List<Map<String, Object>> overrides = leadMinutes.stream()
				.filter(minutes -> minutes <= MAX_REMINDER_MINUTES)
				.limit(MAX_REMINDERS)
				.map(minutes -> Map.<String, Object>of("method", "popup", "minutes", minutes))
				.toList();
		return overrides.isEmpty() ? Map.of("useDefault", true) : Map.of("useDefault", false, "overrides", overrides);
	}
}
