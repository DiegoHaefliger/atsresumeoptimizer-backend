package com.diegohaefliger.atsresumeoptimizer.selection.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.selection.ScheduleStatus;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.InvalidScheduleException;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.ScheduleNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcessNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionSchedule;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionScheduleData;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SelectionScheduleServiceImplTest {

	private static final UUID PROCESS_ID = UUID.randomUUID();
	private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");
	private static final Instant INTERVIEW_AT = Instant.parse("2026-10-08T17:00:00Z");

	@Mock
	private SelectionScheduleRepository repository;

	@Mock
	private SelectionProcessRepository processRepository;

	private final SelectionScheduleData data =
			new SelectionScheduleData(null, INTERVIEW_AT, 60, "  https://meet.example/abc  ", null);

	private SelectionScheduleServiceImpl service() {
		return new SelectionScheduleServiceImpl(repository, processRepository, new SelectionScheduleEntityMapperImpl(),
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private SelectionProcessEntity process(SelectionStage stage) {
		return new SelectionProcessEntity(PROCESS_ID, stage, NOW);
	}

	private SelectionScheduleEntity activeSchedule() {
		SelectionScheduleEntity entity = new SelectionScheduleEntity(PROCESS_ID, SelectionStage.SCREENING, NOW);
		entity.setScheduledAt(INTERVIEW_AT);
		return entity;
	}

	@Test
	void schedulesAtTheCurrentStageWhenNoneIsGiven() {
		when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process(SelectionStage.TECHNICAL_INTERVIEW)));
		when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

		SelectionSchedule created = service().schedule(PROCESS_ID, data);

		assertThat(created.stage()).isEqualTo(SelectionStage.TECHNICAL_INTERVIEW);
		assertThat(created.status()).isEqualTo(ScheduleStatus.SCHEDULED);
		assertThat(created.scheduledAt()).isEqualTo(INTERVIEW_AT);
		assertThat(created.location()).isEqualTo("https://meet.example/abc");
		assertThat(created.createdAt()).isEqualTo(NOW);
	}

	@Test
	void schedulesAtTheGivenStage() {
		when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process(SelectionStage.SCREENING)));
		when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
		SelectionScheduleData withStage =
				new SelectionScheduleData(SelectionStage.MANAGER_INTERVIEW, INTERVIEW_AT, null, null, null);

		assertThat(service().schedule(PROCESS_ID, withStage).stage()).isEqualTo(SelectionStage.MANAGER_INTERVIEW);
	}

	@Test
	void refusesToScheduleForAnUnknownProcess() {
		when(processRepository.findById(PROCESS_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().schedule(PROCESS_ID, data)).isInstanceOf(SelectionProcessNotFoundException.class);

		verify(repository, never()).save(any());
	}

	@Test
	void reschedulingKeepsTheOldOneInTheHistoryAndCreatesANewOne() {
		SelectionScheduleEntity previous = activeSchedule();
		when(processRepository.existsById(PROCESS_ID)).thenReturn(true);
		when(repository.findById(previous.getId())).thenReturn(Optional.of(previous));
		when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
		Instant later = INTERVIEW_AT.plusSeconds(3600);

		SelectionSchedule replacement = service().reschedule(PROCESS_ID, previous.getId(),
				new SelectionScheduleData(null, later, 45, null, null));

		assertThat(previous.getStatus()).isEqualTo(ScheduleStatus.RESCHEDULED);
		assertThat(replacement.id()).isNotEqualTo(previous.getId());
		assertThat(replacement.scheduledAt()).isEqualTo(later);
		assertThat(replacement.stage()).isEqualTo(SelectionStage.SCREENING);
		assertThat(replacement.status()).isEqualTo(ScheduleStatus.SCHEDULED);
	}

	@Test
	void completesAndCancelsActiveSchedules() {
		SelectionScheduleEntity toComplete = activeSchedule();
		SelectionScheduleEntity toCancel = activeSchedule();
		when(processRepository.existsById(PROCESS_ID)).thenReturn(true);
		when(repository.findById(toComplete.getId())).thenReturn(Optional.of(toComplete));
		when(repository.findById(toCancel.getId())).thenReturn(Optional.of(toCancel));

		assertThat(service().complete(PROCESS_ID, toComplete.getId()).status()).isEqualTo(ScheduleStatus.DONE);
		assertThat(service().cancel(PROCESS_ID, toCancel.getId()).status()).isEqualTo(ScheduleStatus.CANCELED);
		assertThat(toCancel.getUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	void refusesToChangeAScheduleThatIsAlreadyClosed() {
		SelectionScheduleEntity closed = activeSchedule();
		closed.setStatus(ScheduleStatus.CANCELED);
		when(processRepository.existsById(PROCESS_ID)).thenReturn(true);
		when(repository.findById(closed.getId())).thenReturn(Optional.of(closed));

		assertThatThrownBy(() -> service().cancel(PROCESS_ID, closed.getId())).isInstanceOf(InvalidScheduleException.class);
		assertThatThrownBy(() -> service().reschedule(PROCESS_ID, closed.getId(), data))
				.isInstanceOf(InvalidScheduleException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void doesNotFindAScheduleOfAnotherProcess() {
		SelectionScheduleEntity foreign = new SelectionScheduleEntity(UUID.randomUUID(), SelectionStage.SCREENING, NOW);
		when(processRepository.existsById(PROCESS_ID)).thenReturn(true);
		when(repository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

		assertThatThrownBy(() -> service().cancel(PROCESS_ID, foreign.getId())).isInstanceOf(ScheduleNotFoundException.class);
	}

	@Test
	void listsTheHistoryOfTheProcess() {
		when(processRepository.existsById(PROCESS_ID)).thenReturn(true);
		when(repository.findByProcessIdOrderByScheduledAtAsc(PROCESS_ID)).thenReturn(List.of(activeSchedule()));

		assertThat(service().list(PROCESS_ID)).hasSize(1);
		verify(repository, times(1)).findByProcessIdOrderByScheduledAtAsc(PROCESS_ID);
	}
}
