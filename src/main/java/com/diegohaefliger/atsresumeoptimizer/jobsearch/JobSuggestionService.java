package com.diegohaefliger.atsresumeoptimizer.jobsearch;

import java.util.List;
import java.util.UUID;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.JobSuggestion;

public interface JobSuggestionService {

	List<JobSuggestion> newSuggestions();

	List<JobSuggestion> belowAdherence();

	List<JobSuggestion> discarded();

	long countNew();

	UUID promote(UUID id);

	JobSuggestion discard(UUID id);

	JobSuggestion restore(UUID id);
}
