package com.diegohaefliger.atsresumeoptimizer.jobsearch.adapter.web;

import org.mapstruct.Mapper;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.JobSuggestion;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SearchProgress;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SearchRun;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionCounts;

@Mapper
interface JobSearchWebMapper {

	JobSuggestionResponse toResponse(JobSuggestion suggestion);

	SearchRunResponse toResponse(SearchRun run);

	SearchProgressResponse toResponse(SearchProgress progress);

	SuggestionCountResponse toResponse(SuggestionCounts counts);
}
