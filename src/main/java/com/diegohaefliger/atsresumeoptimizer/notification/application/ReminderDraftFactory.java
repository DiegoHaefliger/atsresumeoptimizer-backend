package com.diegohaefliger.atsresumeoptimizer.notification.application;

import com.diegohaefliger.atsresumeoptimizer.selection.CalendarEvent;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationDraft;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationType;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
class ReminderDraftFactory {

	private static final DateTimeFormatter WHEN =
			DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm", Locale.forLanguageTag("pt-BR"));
	private static final String SEPARATOR = " · ";

	NotificationDraft create(CalendarEvent event, int leadMinutes, ZoneId zone) {
		String subject = Stream.of(event.company(), event.jobTitle())
				.filter(part -> part != null && !part.isBlank())
				.collect(Collectors.joining(" - "));
		String when = WHEN.format(event.scheduledAt().atZone(zone));
		String message = Stream.of(subject, when, event.location())
				.filter(part -> part != null && !part.isBlank())
				.collect(Collectors.joining(SEPARATOR));
		return new NotificationDraft(NotificationType.SCHEDULE_REMINDER, "Lembrete: " + event.stage().label(), message,
				event.processId(), event.scheduleId(), leadMinutes);
	}
}
