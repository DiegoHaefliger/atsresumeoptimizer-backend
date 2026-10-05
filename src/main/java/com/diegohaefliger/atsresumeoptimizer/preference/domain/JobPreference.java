package com.diegohaefliger.atsresumeoptimizer.preference.domain;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import java.math.BigDecimal;
import java.util.List;

public record JobPreference(
		List<WorkModel> workModels,
		List<ContractType> contractTypes,
		BigDecimal minSalary,
		BigDecimal desiredSalary,
		List<String> benefits,
		List<String> seniorities,
		List<String> locations,
		List<String> preferredCompanies,
		List<String> avoidedCompanies) {

	public static final JobPreference EMPTY =
			new JobPreference(List.of(), List.of(), null, null, List.of(), List.of(), List.of(), List.of(), List.of());

	public JobPreference {
		workModels = workModels == null ? List.of() : workModels.stream().distinct().toList();
		contractTypes = contractTypes == null ? List.of() : contractTypes.stream().distinct().toList();
		benefits = cleaned(benefits);
		seniorities = cleaned(seniorities);
		locations = cleaned(locations);
		preferredCompanies = cleaned(preferredCompanies);
		avoidedCompanies = cleaned(avoidedCompanies);
		if (minSalary != null && desiredSalary != null && desiredSalary.compareTo(minSalary) < 0) {
			throw new InvalidJobPreferenceException("Salário desejado não pode ser menor que o salário mínimo.");
		}
	}

	private static List<String> cleaned(List<String> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream().filter(value -> value != null && !value.isBlank()).map(String::strip).distinct().toList();
	}
}
