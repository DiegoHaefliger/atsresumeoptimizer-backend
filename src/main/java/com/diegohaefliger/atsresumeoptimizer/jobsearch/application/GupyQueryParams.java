package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class GupyQueryParams {

	private static final Map<WorkModel, String> WORK_MODELS =
			Map.of(WorkModel.REMOTE, "remote", WorkModel.HYBRID, "hybrid", WorkModel.ON_SITE, "on-site");
	private static final Map<ContractType, String> CONTRACTS = Map.of(ContractType.CLT, "vacancy_type_effective",
			ContractType.PJ, "vacancy_legal_entity", ContractType.INTERNSHIP, "vacancy_type_internship",
			ContractType.TEMPORARY, "vacancy_type_temporary");
	private static final Set<String> NATIONWIDE = Set.of("brazil", "brasil");

	private GupyQueryParams() {
	}

	static String of(SourceQuery query) {
		List<String> params = new ArrayList<>();
		params.add("term=" + encode(query.keyword()));
		String city = city(query.location());
		if (!city.isEmpty()) {
			params.add("city=" + encode(city));
		}
		query.workModels().stream().map(WORK_MODELS::get).distinct()
				.forEach(code -> params.add("workplaceTypes%5B%5D=" + code));
		query.contractTypes().stream().map(CONTRACTS::get).distinct()
				.forEach(code -> params.add("jobTypes%5B%5D=" + code));
		return String.join("&", params);
	}

	private static String city(String location) {
		String first = location == null ? "" : location.split(",")[0].strip();
		return NATIONWIDE.contains(NormalizedText.of(first)) ? "" : first;
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
	}
}
