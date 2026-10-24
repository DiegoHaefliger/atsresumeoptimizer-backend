package com.diegohaefliger.atsresumeoptimizer.jobsearch;

import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.JobSuggestion;

public interface PostImportService {

	JobSuggestion importPost(String url, String text);
}
