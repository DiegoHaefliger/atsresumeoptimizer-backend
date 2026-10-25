package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import org.mapstruct.Mapper;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SearchRun;

@Mapper
interface SearchRunEntityMapper {

	SearchRun toDomain(SearchRunEntity entity);
}
