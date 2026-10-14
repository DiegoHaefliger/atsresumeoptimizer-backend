package com.diegohaefliger.atsresumeoptimizer.jobnote.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.jobnote.domain.JobNote;
import com.diegohaefliger.atsresumeoptimizer.jobnote.domain.UnknownNoteJobException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobNoteServiceImplTest {

	private static final UUID JOB_ID = UUID.randomUUID();

	@Mock
	private JobNoteRepository repository;

	@Mock
	private JobStructuringService jobService;

	@Mock
	private JobOffer offer;

	private JobNoteServiceImpl service() {
		return new JobNoteServiceImpl(repository, Mappers.getMapper(JobNoteEntityMapper.class), jobService);
	}

	@Test
	void startsEmptyWhenTheJobHasNoNoteYet() {
		when(jobService.offer(JOB_ID)).thenReturn(Optional.of(offer));
		when(repository.findByJobPostingId(JOB_ID)).thenReturn(Optional.empty());

		JobNote note = service().get(JOB_ID);

		assertThat(note.content()).isEmpty();
		assertThat(note.updatedAt()).isNull();
	}

	@Test
	void returnsTheSavedNote() {
		when(jobService.offer(JOB_ID)).thenReturn(Optional.of(offer));
		when(repository.findByJobPostingId(JOB_ID))
				.thenReturn(Optional.of(new JobNoteEntity(JOB_ID, "recrutadora **gentil**", Instant.now())));

		assertThat(service().get(JOB_ID).content()).isEqualTo("recrutadora **gentil**");
	}

	@Test
	void createsTheNoteOnTheFirstSave() {
		when(jobService.offer(JOB_ID)).thenReturn(Optional.of(offer));
		when(repository.findByJobPostingId(JOB_ID)).thenReturn(Optional.empty());
		when(repository.save(any(JobNoteEntity.class))).thenAnswer(call -> call.getArgument(0));

		JobNote note = service().save(JOB_ID, "- empresa em expansão");

		assertThat(note.jobPostingId()).isEqualTo(JOB_ID);
		assertThat(note.content()).isEqualTo("- empresa em expansão");
		assertThat(note.updatedAt()).isNotNull();
	}

	@Test
	void replacesThePreviousTextWithoutKeepingHistory() {
		Instant before = Instant.parse("2026-10-01T10:00:00Z");
		JobNoteEntity entity = new JobNoteEntity(JOB_ID, "antes", before);
		when(jobService.offer(JOB_ID)).thenReturn(Optional.of(offer));
		when(repository.findByJobPostingId(JOB_ID)).thenReturn(Optional.of(entity));
		when(repository.save(entity)).thenReturn(entity);

		JobNote note = service().save(JOB_ID, "depois");

		assertThat(note.content()).isEqualTo("depois");
		assertThat(note.updatedAt()).isAfter(before);
	}

	@Test
	void refusesToSaveANoteForAnUnknownJob() {
		when(jobService.offer(JOB_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().save(JOB_ID, "texto")).isInstanceOf(UnknownNoteJobException.class);
		verify(repository, never()).save(any());
	}
}
