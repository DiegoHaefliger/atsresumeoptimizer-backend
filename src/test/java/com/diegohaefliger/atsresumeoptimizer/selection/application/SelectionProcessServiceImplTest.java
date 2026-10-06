package com.diegohaefliger.atsresumeoptimizer.selection.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.InvalidStageMovementException;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcess;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcessData;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcessNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SelectionProcessServiceImplTest {

	private static final UUID ID = UUID.randomUUID();

	@Mock
	private SelectionProcessRepository repository;

	@Mock
	private SelectionStageMovementRepository movementRepository;

	private final SelectionProcessData data = new SelectionProcessData("Acme", "Dev Java", "https://acme.com/vaga",
			"https://acme.gupy.io/p/1", null, null, null, null, null, "  ótima vaga  ");

	private SelectionProcessServiceImpl service() {
		return new SelectionProcessServiceImpl(repository, movementRepository, new SelectionProcessEntityMapperImpl());
	}

	@Test
	void createsTheProcessAtTheInitialStageAndRecordsTheFirstMovement() {
		when(movementRepository.findByProcessIdOrderByMovedAtAsc(any())).thenReturn(List.of());

		SelectionProcess created = service().create(data, SelectionStage.APPLIED);

		assertThat(created.company()).isEqualTo("Acme");
		assertThat(created.jobUrl()).isEqualTo("https://acme.com/vaga");
		assertThat(created.notes()).isEqualTo("ótima vaga");
		assertThat(created.stage()).isEqualTo(SelectionStage.APPLIED);
		ArgumentCaptor<SelectionStageMovementEntity> movement = ArgumentCaptor.forClass(SelectionStageMovementEntity.class);
		verify(movementRepository).save(movement.capture());
		assertThat(movement.getValue().getStage()).isEqualTo(SelectionStage.APPLIED);
	}

	@Test
	void movesToAnotherStageAndRecordsTheNote() {
		SelectionProcessEntity entity = existing(SelectionStage.SCREENING);
		when(repository.findById(ID)).thenReturn(Optional.of(entity));
		when(movementRepository.findByProcessIdOrderByMovedAtAsc(ID)).thenReturn(List.of());

		SelectionProcess moved = service().moveTo(ID, SelectionStage.TECHNICAL_INTERVIEW, "  marcada para sexta  ");

		assertThat(moved.stage()).isEqualTo(SelectionStage.TECHNICAL_INTERVIEW);
		ArgumentCaptor<SelectionStageMovementEntity> movement = ArgumentCaptor.forClass(SelectionStageMovementEntity.class);
		verify(movementRepository).save(movement.capture());
		assertThat(movement.getValue().getNote()).isEqualTo("marcada para sexta");
	}

	@Test
	void allowsMovingBackToAnEarlierStage() {
		when(repository.findById(ID)).thenReturn(Optional.of(existing(SelectionStage.OFFER)));
		when(movementRepository.findByProcessIdOrderByMovedAtAsc(ID)).thenReturn(List.of());

		assertThat(service().moveTo(ID, SelectionStage.SCREENING, null).stage()).isEqualTo(SelectionStage.SCREENING);
	}

	@Test
	void rejectsMovingToTheCurrentStage() {
		when(repository.findById(ID)).thenReturn(Optional.of(existing(SelectionStage.OFFER)));

		assertThatThrownBy(() -> service().moveTo(ID, SelectionStage.OFFER, null))
				.isInstanceOf(InvalidStageMovementException.class);

		verify(movementRepository, never()).save(any());
	}

	@Test
	void failsWhenTheProcessDoesNotExist() {
		when(repository.findById(ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().get(ID)).isInstanceOf(SelectionProcessNotFoundException.class);
		assertThatThrownBy(() -> service().delete(ID)).isInstanceOf(SelectionProcessNotFoundException.class);
	}

	@Test
	void updateKeepsTheCurrentStage() {
		SelectionProcessEntity entity = existing(SelectionStage.MANAGER_INTERVIEW);
		when(repository.findById(ID)).thenReturn(Optional.of(entity));
		when(movementRepository.findByProcessIdOrderByMovedAtAsc(ID)).thenReturn(List.of());

		SelectionProcess updated = service().update(ID, data);

		assertThat(updated.stage()).isEqualTo(SelectionStage.MANAGER_INTERVIEW);
		assertThat(updated.jobTitle()).isEqualTo("Dev Java");
	}

	@Test
	void filtersTheListByStageWhenGiven() {
		when(repository.findByStageOrderByUpdatedAtDesc(SelectionStage.OFFER))
				.thenReturn(List.of(existing(SelectionStage.OFFER)));

		assertThat(service().list(Optional.of(SelectionStage.OFFER))).hasSize(1);
		verify(repository, never()).findAllByOrderByUpdatedAtDesc();
	}

	private SelectionProcessEntity existing(SelectionStage stage) {
		return new SelectionProcessEntity(ID, stage, Instant.now());
	}
}
