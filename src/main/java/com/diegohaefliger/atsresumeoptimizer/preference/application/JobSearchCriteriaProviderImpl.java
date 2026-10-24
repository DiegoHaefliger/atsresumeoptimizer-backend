package com.diegohaefliger.atsresumeoptimizer.preference.application;

import com.diegohaefliger.atsresumeoptimizer.preference.JobSearchCriteria;
import com.diegohaefliger.atsresumeoptimizer.preference.JobSearchCriteriaProvider;
import com.diegohaefliger.atsresumeoptimizer.preference.domain.JobPreference;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
class JobSearchCriteriaProviderImpl implements JobSearchCriteriaProvider {

	private final JobPreferenceService preferenceService;

	JobSearchCriteriaProviderImpl(JobPreferenceService preferenceService) {
		this.preferenceService = preferenceService;
	}

	@Override
	public JobSearchCriteria current() {
		JobPreference preference = preferenceService.current();
		return new JobSearchCriteria(preference.searchKeywords(), preference.searchExcludedKeywords(),
				preference.seniorities(), preference.locations(), preference.workModels(), preference.contractTypes(),
				preference.searchPostedWithin(), preference.searchMaxResults(), preference.searchMinAdherence(),
				preference.searchEnabled(), Duration.ofHours(preference.searchIntervalHours()));
	}
}
