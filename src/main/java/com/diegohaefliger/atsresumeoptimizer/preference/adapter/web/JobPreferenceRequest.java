package com.diegohaefliger.atsresumeoptimizer.preference.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.preference.PostedWithin;
import com.diegohaefliger.atsresumeoptimizer.preference.SearchSite;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

record JobPreferenceRequest(
		List<WorkModel> workModels,
		List<ContractType> contractTypes,
		@PositiveOrZero BigDecimal minSalary,
		@PositiveOrZero BigDecimal desiredSalary,
		List<@Size(max = 100) String> benefits,
		List<@Size(max = 50) String> seniorities,
		List<@Size(max = 100) String> locations,
		List<@Size(max = 255) String> preferredCompanies,
		List<@Size(max = 255) String> avoidedCompanies,
		List<@Size(max = 100) String> searchKeywords,
		List<@Size(max = 100) String> searchExcludedKeywords,
		PostedWithin searchPostedWithin,
		@Min(1) @Max(100) Integer searchMaxResults,
		@Min(0) @Max(100) Integer searchMinAdherence,
		Boolean searchEnabled,
		@Size(max = 100) String searchCron,
		List<SearchSite> searchSites) {
}
