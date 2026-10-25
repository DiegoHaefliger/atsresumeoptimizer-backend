package com.diegohaefliger.atsresumeoptimizer.preference;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import java.util.ArrayList;
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
		String cron,
		List<SearchSite> sites) {

	public List<String> missingFields() {
		List<String> missing = new ArrayList<>();
		addIfEmpty(missing, keywords, "Cargos e palavras-chave");
		addIfEmpty(missing, seniorities, "Senioridade");
		addIfEmpty(missing, workModels, "Modelo de trabalho");
		addIfEmpty(missing, contractTypes, "Tipo de contrato");
		addIfEmpty(missing, locations, "Localização");
		addIfEmpty(missing, sites, "Sites de busca");
		return List.copyOf(missing);
	}

	private static void addIfEmpty(List<String> missing, List<?> values, String label) {
		if (values.isEmpty()) {
			missing.add(label);
		}
	}
}
