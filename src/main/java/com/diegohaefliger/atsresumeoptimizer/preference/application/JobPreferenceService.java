package com.diegohaefliger.atsresumeoptimizer.preference.application;

import com.diegohaefliger.atsresumeoptimizer.preference.domain.JobPreference;

public interface JobPreferenceService {

	JobPreference current();

	JobPreference save(JobPreference preference);
}
