package com.diegohaefliger.atsresumeoptimizer.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.selection.CalendarEvent;
import com.diegohaefliger.atsresumeoptimizer.selection.ScheduleStatus;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionCalendar;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationDraft;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationSettings;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleReminderJobTest {

	private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");
	private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

	@Mock
	private SelectionCalendar calendar;

	@Mock
	private NotificationSettingsService settingsService;

	@Mock
	private NotificationRepository repository;

	@Mock
	private NotificationDispatcher dispatcher;

	private ScheduleReminderJob job() {
		return new ScheduleReminderJob(calendar, settingsService, repository, dispatcher, new ReminderDraftFactory(),
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private void leads(Integer... minutes) {
		when(settingsService.current()).thenReturn(
				new NotificationSettings(List.of(minutes), Set.of(), ZONE));
	}

	private CalendarEvent eventIn(long minutes) {
		return new CalendarEvent(UUID.randomUUID(), UUID.randomUUID(), "Acme", "Dev Java",
				SelectionStage.TECHNICAL_INTERVIEW, ScheduleStatus.SCHEDULED, NOW.plusSeconds(minutes * 60), 60,
				"https://meet.example/abc", null, null);
	}

	private void pending(CalendarEvent... events) {
		when(calendar.pendingBetween(any(), any())).thenReturn(List.of(events));
	}

	@Test
	void remindsWhenTheLeadTimeIsReached() {
		leads(60, 1440);
		CalendarEvent event = eventIn(55);
		pending(event);
		when(repository.existsByScheduleIdAndLeadMinutes(event.scheduleId(), 60)).thenReturn(false);

		job().dispatchDueReminders();

		ArgumentCaptor<NotificationDraft> draft = ArgumentCaptor.forClass(NotificationDraft.class);
		verify(dispatcher).dispatch(draft.capture());
		assertThat(draft.getValue().scheduleId()).isEqualTo(event.scheduleId());
		assertThat(draft.getValue().leadMinutes()).isEqualTo(60);
		assertThat(draft.getValue().title()).isEqualTo("Lembrete: Entrevista técnica");
		assertThat(draft.getValue().message()).isEqualTo("Acme - Dev Java · 06/10 às 09:55 · https://meet.example/abc");
	}

	@Test
	void doesNotRemindTwiceForTheSameLead() {
		leads(60, 1440);
		CalendarEvent event = eventIn(55);
		pending(event);
		when(repository.existsByScheduleIdAndLeadMinutes(event.scheduleId(), 60)).thenReturn(true);

		job().dispatchDueReminders();

		verify(dispatcher, never()).dispatch(any());
	}

	@Test
	void usesTheFurtherLeadWhileTheNearerOneIsNotDue() {
		leads(60, 1440);
		CalendarEvent event = eventIn(600);
		pending(event);
		when(repository.existsByScheduleIdAndLeadMinutes(event.scheduleId(), 1440)).thenReturn(false);

		job().dispatchDueReminders();

		ArgumentCaptor<NotificationDraft> draft = ArgumentCaptor.forClass(NotificationDraft.class);
		verify(dispatcher).dispatch(draft.capture());
		assertThat(draft.getValue().leadMinutes()).isEqualTo(1440);
	}

	@Test
	void aScheduleCreatedInsideEveryWindowRemindsOnlyOnceWithTheNearestLead() {
		leads(15, 60, 1440);
		CalendarEvent event = eventIn(10);
		pending(event);
		when(repository.existsByScheduleIdAndLeadMinutes(event.scheduleId(), 15)).thenReturn(false);

		job().dispatchDueReminders();

		ArgumentCaptor<NotificationDraft> draft = ArgumentCaptor.forClass(NotificationDraft.class);
		verify(dispatcher).dispatch(draft.capture());
		assertThat(draft.getValue().leadMinutes()).isEqualTo(15);
	}

	@Test
	void doesNothingWhenNoEventIsPending() {
		leads(60);
		pending();

		job().dispatchDueReminders();

		verifyNoInteractions(dispatcher, repository);
	}
}
