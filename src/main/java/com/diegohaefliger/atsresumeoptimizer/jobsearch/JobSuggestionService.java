package com.diegohaefliger.atsresumeoptimizer.jobsearch;

import java.util.UUID;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.JobSuggestion;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionCounts;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionPage;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionQuery;

public interface JobSuggestionService {

	SuggestionPage newSuggestions(SuggestionQuery query);

	SuggestionPage belowAdherence(SuggestionQuery query);

	SuggestionPage discarded(SuggestionQuery query);

	SuggestionCounts counts();

	UUID promote(UUID id);

	JobSuggestion discard(UUID id);

	JobSuggestion restore(UUID id);
}
