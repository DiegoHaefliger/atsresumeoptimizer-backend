package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.preference.PostedWithin;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

final class LinkedInQueryParams {

	private static final Map<WorkModel, String> WORK_MODEL_CODES =
			Map.of(WorkModel.ON_SITE, "1", WorkModel.REMOTE, "2", WorkModel.HYBRID, "3");
	private static final Map<ContractType, String> CONTRACT_CODES = Map.of(ContractType.CLT, "F", ContractType.PJ, "C",
			ContractType.INTERNSHIP, "I", ContractType.TEMPORARY, "T");
	private static final Map<String, String> EXPERIENCE_CODES = Map.of("junior", "2", "pleno", "3", "senior", "4",
			"especialista", "4", "tech lead", "4");
	private static final Map<PostedWithin, String> POSTED_CODES = Map.of(PostedWithin.PAST_HOUR, "r3600",
			PostedWithin.PAST_2_HOURS, "r7200", PostedWithin.PAST_6_HOURS, "r21600", PostedWithin.PAST_12_HOURS, "r43200",
			PostedWithin.PAST_24_HOURS, "r86400", PostedWithin.PAST_WEEK, "r604800", PostedWithin.PAST_MONTH, "r2592000");

	private LinkedInQueryParams() {
	}

	static Map<String, String> of(SourceQuery query) {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("keywords", query.keyword());
		params.put("location", query.location());
		put(params, "f_TPR", POSTED_CODES.get(query.postedWithin()));
		put(params, "f_WT", joined(query.workModels().stream().map(WORK_MODEL_CODES::get).toList()));
		put(params, "f_JT", joined(query.contractTypes().stream().map(CONTRACT_CODES::get).toList()));
		put(params, "f_E", joined(query.seniorities().stream()
				.map(level -> EXPERIENCE_CODES.get(NormalizedText.of(level))).toList()));
		params.put("start", String.valueOf(query.start()));
		return params;
	}

	private static void put(Map<String, String> params, String name, String value) {
		if (value != null && !value.isEmpty()) {
			params.put(name, value);
		}
	}

	private static String joined(List<String> codes) {
		return codes.stream().filter(Objects::nonNull).distinct().collect(Collectors.joining(","));
	}
}
