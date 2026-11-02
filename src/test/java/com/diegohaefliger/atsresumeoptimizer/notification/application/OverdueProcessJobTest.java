package com.diegohaefliger.atsresumeoptimizer.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationChannel;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationDraft;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationSettings;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationType;
import com.diegohaefliger.atsresumeoptimizer.selection.ScheduleStatus;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import com.diegohaefliger.atsresumeoptimizer.selection.application.SelectionProcessService;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcess;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionSchedule;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OverdueProcessJobTest {

	private static final Instant NOW = Instant.parse("2026-10-30T15:00:00Z");
	private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

	@Mock
	private SelectionProcessService processes;

	@Mock
	private NotificationSettingsService settingsService;

	@Mock
	private NotificationRepository repository;

	@Mock
	private NotificationDispatcher dispatcher;

	private OverdueProcessJob job() {
		return new OverdueProcessJob(processes, settingsService, repository, dispatcher, new OverdueDraftFactory(),
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private void channels(NotificationChannel... channels) {
		when(settingsService.current()).thenReturn(new NotificationSettings(List.of(60), Set.of(channels), ZONE));
	}

	private SelectionProcess process(SelectionStage stage, LocalDate nextStepOn, SelectionSchedule next) {
		UUID id = next != null ? next.processId() : UUID.randomUUID();
		return new SelectionProcess(id, UUID.randomUUID(), 12L, "Acme", "Dev Java", null, null, stage, null, nextStepOn,
				null, null, null, null, null, NOW, NOW.minusSeconds(86_400 * 5), List.of(), List.of(), next, null);
	}

	private SelectionSchedule scheduleAt(Instant at) {
		return new SelectionSchedule(UUID.randomUUID(), UUID.randomUUID(), SelectionStage.TECHNICAL_INTERVIEW, at, 60, null,
				null, ScheduleStatus.SCHEDULED, NOW, NOW);
	}

	@Test
	void notifiesAScheduleThatAlreadyHappenedWithoutBeingRecorded() {
		channels(NotificationChannel.IN_APP);
		SelectionSchedule missed = scheduleAt(NOW.minusSeconds(3_600));
		when(processes.list(Optional.empty())).thenReturn(List.of(process(SelectionStage.HR_INTERVIEW, null, missed)));

		job().notifyOverdueProcesses();

		ArgumentCaptor<NotificationDraft> draft = ArgumentCaptor.forClass(NotificationDraft.class);
		verify(dispatcher).dispatch(draft.capture());
		assertThat(draft.getValue().type()).isEqualTo(NotificationType.SCHEDULE_OVERDUE);
		assertThat(draft.getValue().title()).isEqualTo("Etapa atrasada: Entrevista técnica");
		assertThat(draft.getValue().message()).isEqualTo("Acme - Dev Java · era 30/10 às 11:00 e ainda está como agendada. Atualize o processo.");
		assertThat(draft.getValue().scheduleId()).isEqualTo(missed.id());
	}

	@Test
	void doesNotRepeatAnOverdueScheduleAlreadyNotified() {
		channels(NotificationChannel.IN_APP);
		SelectionSchedule missed = scheduleAt(NOW.minusSeconds(3_600));
		when(processes.list(Optional.empty())).thenReturn(List.of(process(SelectionStage.HR_INTERVIEW, null, missed)));
		when(repository.existsByTypeAndScheduleId(NotificationType.SCHEDULE_OVERDUE, missed.id())).thenReturn(true);

		job().notifyOverdueProcesses();

		verify(dispatcher, never()).dispatch(any());
	}

	@Test
	void notifiesAPastNextStepOncePerProcessUpdate() {
		channels(NotificationChannel.IN_APP);
		SelectionProcess late = process(SelectionStage.APPLIED, LocalDate.of(2026, 10, 27), null);
		when(processes.list(Optional.empty())).thenReturn(List.of(late));

		job().notifyOverdueProcesses();

		ArgumentCaptor<NotificationDraft> draft = ArgumentCaptor.forClass(NotificationDraft.class);
		verify(dispatcher).dispatch(draft.capture());
		assertThat(draft.getValue().type()).isEqualTo(NotificationType.NEXT_STEP_OVERDUE);
		assertThat(draft.getValue().message()).isEqualTo("Acme - Dev Java · o próximo passo era 27/10. Atualize o processo.");
		verify(repository).existsByTypeAndProcessIdAndCreatedAtAfter(NotificationType.NEXT_STEP_OVERDUE, late.id(),
				late.updatedAt());
	}

	@Test
	void ignoresClosedProcessesFutureSchedulesAndTodaysNextStep() {
		channels(NotificationChannel.IN_APP);
		when(processes.list(Optional.empty())).thenReturn(List.of(
				process(SelectionStage.REJECTED, null, scheduleAt(NOW.minusSeconds(3_600))),
				process(SelectionStage.SCREENING, null, scheduleAt(NOW.plusSeconds(3_600))),
				process(SelectionStage.APPLIED, LocalDate.of(2026, 10, 30), null)));

		job().notifyOverdueProcesses();

		verify(dispatcher, never()).dispatch(any());
	}

	@Test
	void staysQuietWithoutTheInAppChannelToRememberWhatWasSent() {
		channels(NotificationChannel.EMAIL);

		job().notifyOverdueProcesses();

		verifyNoInteractions(processes, dispatcher);
	}
}
