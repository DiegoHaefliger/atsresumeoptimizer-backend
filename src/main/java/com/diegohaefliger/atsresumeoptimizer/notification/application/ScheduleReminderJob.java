package com.diegohaefliger.atsresumeoptimizer.notification.application;

import com.diegohaefliger.atsresumeoptimizer.selection.CalendarEvent;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionCalendar;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationSettings;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class ScheduleReminderJob {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleReminderJob.class);

	private final SelectionCalendar calendar;
	private final NotificationSettingsService settingsService;
	private final NotificationRepository repository;
	private final NotificationDispatcher dispatcher;
	private final ReminderDraftFactory draftFactory;
	private final Clock clock;

	ScheduleReminderJob(SelectionCalendar calendar, NotificationSettingsService settingsService,
			NotificationRepository repository, NotificationDispatcher dispatcher, ReminderDraftFactory draftFactory,
			Clock clock) {
		this.calendar = calendar;
		this.settingsService = settingsService;
		this.repository = repository;
		this.dispatcher = dispatcher;
		this.draftFactory = draftFactory;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${app.notification.reminder-interval:PT1M}")
	void dispatchDueReminders() {
		Instant now = Instant.now(clock);
		NotificationSettings settings = settingsService.current();
		List<Integer> leads = settings.leadMinutes();
		if (leads.isEmpty()) {
			return;
		}
		Instant horizon = now.plus(Duration.ofMinutes(leads.getLast()));
		for (CalendarEvent event : calendar.pendingBetween(now, horizon)) {
			dueLead(event, leads, now)
					.filter(lead -> !repository.existsByScheduleIdAndLeadMinutes(event.scheduleId(), lead))
					.ifPresent(lead -> {
						dispatcher.dispatch(draftFactory.create(event, lead, settings.timezone()));
						LOGGER.info("Lembrete enviado: agendamento {} ({} min antes)", event.scheduleId(), lead);
					});
		}
	}

	private static Optional<Integer> dueLead(CalendarEvent event, List<Integer> sortedLeads, Instant now) {
		Duration remaining = Duration.between(now, event.scheduledAt());
		return sortedLeads.stream().filter(lead -> remaining.compareTo(Duration.ofMinutes(lead)) <= 0).findFirst();
	}
}
