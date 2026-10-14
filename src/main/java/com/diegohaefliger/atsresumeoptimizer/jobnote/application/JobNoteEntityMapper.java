package com.diegohaefliger.atsresumeoptimizer.jobnote.application;

import com.diegohaefliger.atsresumeoptimizer.jobnote.domain.JobNote;
import org.mapstruct.Mapper;

@Mapper
interface JobNoteEntityMapper {

	JobNote toDomain(JobNoteEntity entity);
}
