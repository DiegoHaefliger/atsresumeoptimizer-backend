package com.diegohaefliger.atsresumeoptimizer.personalnote.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.personalnote.domain.PersonalNote;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersonalNoteServiceImplTest {

	@Mock
	private PersonalNoteRepository repository;

	private PersonalNoteServiceImpl service() {
		return new PersonalNoteServiceImpl(repository, Mappers.getMapper(PersonalNoteEntityMapper.class));
	}

	@Test
	void startsEmptyWhenNothingWasSavedYet() {
		when(repository.findById(PersonalNoteEntity.SINGLE_ID)).thenReturn(Optional.empty());

		PersonalNote note = service().get();

		assertThat(note.content()).isEmpty();
		assertThat(note.updatedAt()).isNull();
	}

	@Test
	void createsTheNoteOnTheFirstSave() {
		when(repository.findById(PersonalNoteEntity.SINGLE_ID)).thenReturn(Optional.empty());
		when(repository.save(any(PersonalNoteEntity.class))).thenAnswer(call -> call.getArgument(0));

		PersonalNote note = service().save("**organizado** e pontual");

		assertThat(note.content()).isEqualTo("**organizado** e pontual");
		assertThat(note.updatedAt()).isNotNull();
	}

	@Test
	void replacesThePreviousTextWithoutKeepingHistory() {
		Instant before = Instant.parse("2026-10-01T10:00:00Z");
		PersonalNoteEntity entity = new PersonalNoteEntity("texto antigo", before);
		when(repository.findById(PersonalNoteEntity.SINGLE_ID)).thenReturn(Optional.of(entity));
		when(repository.save(entity)).thenReturn(entity);

		PersonalNote note = service().save("texto novo");

		assertThat(note.content()).isEqualTo("texto novo");
		assertThat(note.updatedAt()).isAfter(before);
	}

	@Test
	void acceptsAnEmptyTextToClearTheNote() {
		PersonalNoteEntity entity = new PersonalNoteEntity("algo", Instant.now());
		when(repository.findById(PersonalNoteEntity.SINGLE_ID)).thenReturn(Optional.of(entity));
		when(repository.save(entity)).thenReturn(entity);

		assertThat(service().save("").content()).isEmpty();
	}
}
