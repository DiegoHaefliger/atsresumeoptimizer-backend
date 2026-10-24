package com.diegohaefliger.atsresumeoptimizer.jobsearch.adapter.web;

import org.mapstruct.Mapper;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.JobSuggestion;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SearchRun;

@Mapper
interface JobSearchWebMapper {

	JobSuggestionResponse toResponse(JobSuggestion suggestion);

	SearchRunResponse toResponse(SearchRun run);
}
