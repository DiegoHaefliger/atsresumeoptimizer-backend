package com.diegohaefliger.atsresumeoptimizer.personalnote.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.personalnote.domain.PersonalNote;
import org.mapstruct.Mapper;

@Mapper
interface PersonalNoteWebMapper {

	PersonalNoteResponse toResponse(PersonalNote note);
}
