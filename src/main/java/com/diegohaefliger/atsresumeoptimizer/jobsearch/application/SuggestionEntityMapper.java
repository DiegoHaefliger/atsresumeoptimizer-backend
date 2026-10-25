package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import org.mapstruct.Mapper;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.JobSuggestion;

@Mapper
interface SuggestionEntityMapper {

	JobSuggestion toDomain(SuggestionEntity entity);
}
