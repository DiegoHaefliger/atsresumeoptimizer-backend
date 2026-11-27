package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.diagnostics.ReadinessCheck;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.ReadinessContributor;
import com.diegohaefliger.atsresumeoptimizer.preference.JobSearchCriteriaProvider;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(50)
class JobSearchReadiness implements ReadinessContributor {

	private static final String NAME = "Busca automática de vagas";

	private final JobSearchCriteriaProvider criteriaProvider;

	JobSearchReadiness(JobSearchCriteriaProvider criteriaProvider) {
		this.criteriaProvider = criteriaProvider;
	}

	@Override
	public List<ReadinessCheck> checks() {
		List<String> missing = criteriaProvider.current().missingFields();
		return List.of(missing.isEmpty()
				? ReadinessCheck.ok(NAME, "preferências completas")
				: ReadinessCheck.warning(NAME, "faltam " + String.join(", ", missing),
						"Preencha em Preferências > Busca de vagas pra buscar vagas automaticamente."));
	}
}
