package com.diegohaefliger.atsresumeoptimizer.personalnote.application;

import com.diegohaefliger.atsresumeoptimizer.personalnote.domain.PersonalNote;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PersonalNoteServiceImpl implements PersonalNoteService {

	private final PersonalNoteRepository repository;
	private final PersonalNoteEntityMapper mapper;

	PersonalNoteServiceImpl(PersonalNoteRepository repository, PersonalNoteEntityMapper mapper) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Override
	@Transactional(readOnly = true)
	public PersonalNote get() {
		return repository.findById(PersonalNoteEntity.SINGLE_ID).map(mapper::toDomain).orElseGet(PersonalNote::empty);
	}

	@Override
	@Transactional
	public PersonalNote save(String content) {
		Instant now = Instant.now();
		PersonalNoteEntity entity = repository.findById(PersonalNoteEntity.SINGLE_ID)
				.orElseGet(() -> new PersonalNoteEntity(content, now));
		entity.replace(content, now);
		return mapper.toDomain(repository.save(entity));
	}
}
