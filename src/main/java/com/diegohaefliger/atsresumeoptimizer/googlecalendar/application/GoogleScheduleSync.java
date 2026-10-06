package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import com.diegohaefliger.atsresumeoptimizer.notification.ReminderLeadTimes;
import com.diegohaefliger.atsresumeoptimizer.selection.CalendarEvent;
import com.diegohaefliger.atsresumeoptimizer.selection.ScheduleChanged;
import com.diegohaefliger.atsresumeoptimizer.selection.ScheduleStatus;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionCalendar;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleAuthRevokedException;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleCredentials;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class GoogleScheduleSync {

	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleScheduleSync.class);
	private static final Duration SYNC_WINDOW = Duration.ofDays(365);

	private final GoogleAccountStore accountStore;
	private final GoogleCalendarGateway gateway;
	private final GoogleEventLinkRepository linkRepository;
	private final GoogleEventPayloads payloads;
	private final SelectionCalendar calendar;
	private final ReminderLeadTimes leadTimes;
	private final Clock clock;

	GoogleScheduleSync(GoogleAccountStore accountStore, GoogleCalendarGateway gateway,
			GoogleEventLinkRepository linkRepository, GoogleEventPayloads payloads, SelectionCalendar calendar,
			ReminderLeadTimes leadTimes, Clock clock) {
		this.accountStore = accountStore;
		this.gateway = gateway;
		this.linkRepository = linkRepository;
		this.payloads = payloads;
		this.calendar = calendar;
		this.leadTimes = leadTimes;
		this.clock = clock;
	}

	@ApplicationModuleListener
	void onScheduleChanged(ScheduleChanged event) {
		try {
			syncOne(event.scheduleId());
		} catch (RuntimeException exception) {
			LOGGER.warn("Sincronização com o Google falhou para o agendamento {}: {}", event.scheduleId(),
					exception.getMessage());
		}
	}

	@Transactional
	void syncOne(UUID scheduleId) {
		Optional<String> accessToken = accessToken();
		if (accessToken.isEmpty()) {
			return;
		}
		Optional<CalendarEvent> event = calendar.find(scheduleId);
		if (event.isPresent()) {
			apply(accessToken.get(), event.get(), leadTimes.leadMinutes());
		} else {
			linkRepository.findById(scheduleId).ifPresent(link -> remove(accessToken.get(), scheduleId, link));
		}
	}

	@Transactional
	int syncAll() {
		Optional<String> accessToken = accessToken();
		if (accessToken.isEmpty()) {
			return 0;
		}
		Instant now = Instant.now(clock);
		List<Integer> leads = leadTimes.leadMinutes();
		List<CalendarEvent> events = calendar.between(now, now.plus(SYNC_WINDOW));
		events.forEach(event -> apply(accessToken.get(), event, leads));
		return events.size();
	}

	private void apply(String accessToken, CalendarEvent event, List<Integer> leads) {
		Optional<GoogleEventLinkEntity> link = linkRepository.findById(event.scheduleId());
		if (event.status() == ScheduleStatus.SCHEDULED || event.status() == ScheduleStatus.DONE) {
			String eventId = gateway.saveEvent(accessToken, link.map(GoogleEventLinkEntity::getGoogleEventId).orElse(null),
					payloads.create(event, leads));
			GoogleEventLinkEntity saved = link.orElseGet(() -> new GoogleEventLinkEntity(event.scheduleId(), eventId, null));
			saved.setGoogleEventId(eventId);
			saved.setSyncedAt(Instant.now(clock));
			linkRepository.save(saved);
		} else {
			link.ifPresent(existing -> remove(accessToken, event.scheduleId(), existing));
		}
	}

	private void remove(String accessToken, UUID scheduleId, GoogleEventLinkEntity link) {
		gateway.deleteEvent(accessToken, link.getGoogleEventId());
		linkRepository.deleteById(scheduleId);
	}

	private Optional<String> accessToken() {
		Optional<GoogleCredentials> credentials = accountStore.credentials();
		Optional<String> refreshToken = accountStore.refreshToken();
		if (credentials.isEmpty() || refreshToken.isEmpty()) {
			return Optional.empty();
		}
		try {
			return Optional.of(gateway.refreshAccessToken(credentials.get(), refreshToken.get()));
		} catch (GoogleAuthRevokedException exception) {
			LOGGER.warn("Acesso ao Google revogado; conta desconectada.");
			accountStore.clearConnection();
			return Optional.empty();
		}
	}
}
