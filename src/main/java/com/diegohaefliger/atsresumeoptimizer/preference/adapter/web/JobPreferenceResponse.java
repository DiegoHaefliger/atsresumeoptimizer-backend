package com.diegohaefliger.atsresumeoptimizer.preference.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import java.math.BigDecimal;
import java.util.List;

record JobPreferenceResponse(
		List<WorkModel> workModels,
		List<ContractType> contractTypes,
		BigDecimal minSalary,
		BigDecimal desiredSalary,
		List<String> benefits,
		List<String> seniorities,
		List<String> locations,
		List<String> preferredCompanies,
		List<String> avoidedCompanies) {
}
