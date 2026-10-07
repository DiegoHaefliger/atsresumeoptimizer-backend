package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.notification.ReminderLeadTimes;
import com.diegohaefliger.atsresumeoptimizer.selection.CalendarEvent;
import com.diegohaefliger.atsresumeoptimizer.selection.ScheduleStatus;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionCalendar;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoogleScheduleSyncTest {

	private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");
	private static final UUID SCHEDULE_ID = UUID.randomUUID();

	@Mock
	private GoogleAccessTokens accessTokens;

	@Mock
	private GoogleCalendarGateway gateway;

	@Mock
	private GoogleEventLinkRepository linkRepository;

	@Mock
	private SelectionCalendar calendar;

	@Mock
	private ReminderLeadTimes leadTimes;

	private final GoogleCalendarProperties properties = new GoogleCalendarProperties("id", "secret", "http://localhost:8080/cb",
			"http://localhost:5173", "a", "t", "r", "u", "e");

	private GoogleScheduleSync sync() {
		return new GoogleScheduleSync(accessTokens, gateway, linkRepository, new GoogleEventPayloads(properties), calendar,
				leadTimes, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private void connected() {
		when(accessTokens.current()).thenReturn(Optional.of("acc"));
	}

	private CalendarEvent event(ScheduleStatus status) {
		return new CalendarEvent(SCHEDULE_ID, UUID.randomUUID(), "Acme", "Dev Java", SelectionStage.TECHNICAL_INTERVIEW,
				status, NOW.plusSeconds(3600), 30, "Sala 3", "levar RG");
	}

	@Test
	void doesNothingWhenNoGoogleAccountIsConnected() {
		when(accessTokens.current()).thenReturn(Optional.empty());

		sync().syncOne(SCHEDULE_ID);

		verifyNoInteractions(gateway, calendar, linkRepository);
	}


	@Test
	void createsTheGoogleEventAndRemembersItsId() {
		connected();
		when(leadTimes.leadMinutes()).thenReturn(List.of(60, 1440));
		when(calendar.find(SCHEDULE_ID)).thenReturn(Optional.of(event(ScheduleStatus.SCHEDULED)));
		when(linkRepository.findById(SCHEDULE_ID)).thenReturn(Optional.empty());
		when(gateway.saveEvent(eq("acc"), eq(null), any())).thenReturn("evt1");

		sync().syncOne(SCHEDULE_ID);

		org.mockito.ArgumentCaptor<Map<String, Object>> body = org.mockito.ArgumentCaptor.forClass(Map.class);
		verify(gateway).saveEvent(eq("acc"), eq(null), body.capture());
		assertThat(body.getValue()).containsEntry("summary", "Entrevista técnica: Acme - Dev Java")
				.containsEntry("location", "Sala 3");
		assertThat(body.getValue().get("description").toString()).contains("levar RG", "http://localhost:5173/processes/");
		assertThat(body.getValue().get("end")).isEqualTo(Map.of("dateTime", NOW.plusSeconds(3600 + 1800).toString()));
		assertThat(body.getValue().get("reminders").toString()).contains("minutes=60", "minutes=1440");
		verify(linkRepository).save(any());
	}

	@Test
	void updatesTheLinkedEventInPlace() {
		connected();
		when(leadTimes.leadMinutes()).thenReturn(List.of());
		when(calendar.find(SCHEDULE_ID)).thenReturn(Optional.of(event(ScheduleStatus.SCHEDULED)));
		when(linkRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(new GoogleEventLinkEntity(SCHEDULE_ID, "evt1", NOW)));
		when(gateway.saveEvent(eq("acc"), eq("evt1"), any())).thenReturn("evt1");

		sync().syncOne(SCHEDULE_ID);

		verify(gateway).saveEvent(eq("acc"), eq("evt1"), any());
	}

	@Test
	void removesTheGoogleEventWhenTheScheduleIsCanceledOrRescheduled() {
		connected();
		when(calendar.find(SCHEDULE_ID)).thenReturn(Optional.of(event(ScheduleStatus.CANCELED)));
		when(linkRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(new GoogleEventLinkEntity(SCHEDULE_ID, "evt1", NOW)));

		sync().syncOne(SCHEDULE_ID);

		verify(gateway).deleteEvent("acc", "evt1");
		verify(linkRepository).deleteById(SCHEDULE_ID);
		verify(gateway, never()).saveEvent(any(), any(), any());
	}

	@Test
	void removesTheGoogleEventWhenTheScheduleNoLongerExists() {
		connected();
		when(calendar.find(SCHEDULE_ID)).thenReturn(Optional.empty());
		when(linkRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(new GoogleEventLinkEntity(SCHEDULE_ID, "evt1", NOW)));

		sync().syncOne(SCHEDULE_ID);

		verify(gateway).deleteEvent("acc", "evt1");
	}


	@Test
	void syncAllPushesEveryEventOfTheNextYear() {
		connected();
		when(leadTimes.leadMinutes()).thenReturn(List.of(60));
		when(calendar.between(any(), any())).thenReturn(List.of(event(ScheduleStatus.SCHEDULED), event(ScheduleStatus.DONE)));
		when(linkRepository.findById(SCHEDULE_ID)).thenReturn(Optional.empty());
		when(gateway.saveEvent(any(), any(), any())).thenReturn("evt");

		assertThat(sync().syncAll()).isEqualTo(2);
	}
}
