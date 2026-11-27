package com.diegohaefliger.atsresumeoptimizer.jobsearch.adapter.web;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.JobSuggestion;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SearchProgress;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SearchRun;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionCounts;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionPage;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionQuery;

@Mapper
interface JobSearchWebMapper {

	JobSuggestionResponse toResponse(JobSuggestion suggestion);

	JobSuggestionPageResponse toResponse(SuggestionPage page);

	@Mapping(target = "text", source = "query")
	SuggestionQuery toQuery(SuggestionPageRequest request);

	SearchRunResponse toResponse(SearchRun run);

	SearchProgressResponse toResponse(SearchProgress progress);

	SuggestionCountResponse toResponse(SuggestionCounts counts);
}
