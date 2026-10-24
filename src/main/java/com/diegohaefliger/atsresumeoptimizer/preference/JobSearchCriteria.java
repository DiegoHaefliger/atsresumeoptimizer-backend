package com.diegohaefliger.atsresumeoptimizer.preference;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import java.time.Duration;
import java.util.List;

public record JobSearchCriteria(
		List<String> keywords,
		List<String> excludedKeywords,
		List<String> seniorities,
		List<String> locations,
		List<WorkModel> workModels,
		List<ContractType> contractTypes,
		PostedWithin postedWithin,
		int maxResults,
		int minAdherence,
		boolean enabled,
		Duration interval) {
}
