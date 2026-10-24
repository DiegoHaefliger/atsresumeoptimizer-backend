package com.diegohaefliger.atsresumeoptimizer.preference.domain;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.preference.PostedWithin;
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
		List<String> avoidedCompanies,
		List<String> searchKeywords,
		List<String> searchExcludedKeywords,
		PostedWithin searchPostedWithin,
		Integer searchMaxResults,
		Integer searchMinAdherence,
		Boolean searchEnabled,
		Integer searchIntervalHours) {

	public static final int DEFAULT_MAX_RESULTS = 50;
	public static final int MAX_RESULTS_LIMIT = 100;
	public static final int DEFAULT_MIN_ADHERENCE = 70;
	public static final int MAX_ADHERENCE = 100;
	public static final int DEFAULT_INTERVAL_HOURS = 12;
	public static final List<Integer> INTERVAL_HOURS_OPTIONS = List.of(6, 12, 24);

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
		searchKeywords = cleaned(searchKeywords);
		searchExcludedKeywords = cleaned(searchExcludedKeywords);
		searchPostedWithin = searchPostedWithin == null ? PostedWithin.PAST_WEEK : searchPostedWithin;
		searchMaxResults = searchMaxResults == null ? DEFAULT_MAX_RESULTS : searchMaxResults;
		searchMinAdherence = searchMinAdherence == null ? DEFAULT_MIN_ADHERENCE : searchMinAdherence;
		searchEnabled = searchEnabled != null && searchEnabled;
		searchIntervalHours = searchIntervalHours == null ? DEFAULT_INTERVAL_HOURS : searchIntervalHours;
		if (minSalary != null && desiredSalary != null && desiredSalary.compareTo(minSalary) < 0) {
			throw new InvalidJobPreferenceException("Salário desejado não pode ser menor que o salário mínimo.");
		}
		if (searchMaxResults < 1 || searchMaxResults > MAX_RESULTS_LIMIT) {
			throw new InvalidJobPreferenceException(
					"O limite de vagas por busca deve ficar entre 1 e " + MAX_RESULTS_LIMIT + ".");
		}
		if (searchMinAdherence < 0 || searchMinAdherence > MAX_ADHERENCE) {
			throw new InvalidJobPreferenceException("A aderência mínima deve ficar entre 0 e " + MAX_ADHERENCE + ".");
		}
		if (!INTERVAL_HOURS_OPTIONS.contains(searchIntervalHours)) {
			throw new InvalidJobPreferenceException("O intervalo da busca deve ser de 6, 12 ou 24 horas.");
		}
		if (searchEnabled && searchKeywords.isEmpty()) {
			throw new InvalidJobPreferenceException("Informe ao menos um cargo ou palavra-chave para ativar a busca.");
		}
	}

	public JobPreference(List<WorkModel> workModels, List<ContractType> contractTypes, BigDecimal minSalary,
			BigDecimal desiredSalary, List<String> benefits, List<String> seniorities, List<String> locations,
			List<String> preferredCompanies, List<String> avoidedCompanies) {
		this(workModels, contractTypes, minSalary, desiredSalary, benefits, seniorities, locations, preferredCompanies,
				avoidedCompanies, null, null, null, null, null, null, null);
	}

	private static List<String> cleaned(List<String> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream().filter(value -> value != null && !value.isBlank()).map(String::strip).distinct().toList();
	}
}
