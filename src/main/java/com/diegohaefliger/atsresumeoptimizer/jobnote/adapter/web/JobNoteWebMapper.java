package com.diegohaefliger.atsresumeoptimizer.jobnote.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.jobnote.domain.JobNote;
import org.mapstruct.Mapper;

@Mapper
interface JobNoteWebMapper {

	JobNoteResponse toResponse(JobNote note);
}
