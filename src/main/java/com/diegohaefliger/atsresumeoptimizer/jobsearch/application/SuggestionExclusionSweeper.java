package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionStatus;
import com.diegohaefliger.atsresumeoptimizer.preference.JobPreferenceSaved;
import com.diegohaefliger.atsresumeoptimizer.preference.JobSearchCriteriaProvider;
import org.springframework.data.domain.Sort;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class SuggestionExclusionSweeper {

	private final SuggestionRepository repository;
	private final JobSearchCriteriaProvider criteriaProvider;

	SuggestionExclusionSweeper(SuggestionRepository repository, JobSearchCriteriaProvider criteriaProvider) {
		this.repository = repository;
		this.criteriaProvider = criteriaProvider;
	}

	@ApplicationModuleListener
	void onPreferenceSaved(JobPreferenceSaved event) {
		ExcludedTerms excluded = new ExcludedTerms(criteriaProvider.current().excludedKeywords());
		if (excluded.isEmpty()) {
			return;
		}
		repository.findByStatus(SuggestionStatus.NEW, Sort.unsorted()).stream()
				.filter(suggestion -> excluded.appearIn(suggestion.getTitle(), suggestion.getCompany(), suggestion.getDescription()))
				.forEach(suggestion -> {
					suggestion.setStatus(SuggestionStatus.FILTERED);
					repository.save(suggestion);
				});
	}
}
