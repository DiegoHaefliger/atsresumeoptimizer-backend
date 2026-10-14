package com.diegohaefliger.atsresumeoptimizer.personalnote.application;

import com.diegohaefliger.atsresumeoptimizer.personalnote.domain.PersonalNote;
import org.mapstruct.Mapper;

@Mapper
interface PersonalNoteEntityMapper {

	PersonalNote toDomain(PersonalNoteEntity entity);
}
