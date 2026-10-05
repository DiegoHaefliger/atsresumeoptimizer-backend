package com.diegohaefliger.atsresumeoptimizer.preference.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
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
		List<@Size(max = 255) String> avoidedCompanies) {
}
