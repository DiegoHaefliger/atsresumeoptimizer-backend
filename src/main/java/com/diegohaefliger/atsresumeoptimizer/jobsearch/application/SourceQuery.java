package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.preference.PostedWithin;
import java.util.List;

record SourceQuery(
		String keyword,
		String location,
		PostedWithin postedWithin,
		List<WorkModel> workModels,
		List<String> seniorities,
		List<ContractType> contractTypes,
		int start) {

	SourceQuery nextPage(int pageSize) {
		return new SourceQuery(keyword, location, postedWithin, workModels, seniorities, contractTypes, start + pageSize);
	}
}
