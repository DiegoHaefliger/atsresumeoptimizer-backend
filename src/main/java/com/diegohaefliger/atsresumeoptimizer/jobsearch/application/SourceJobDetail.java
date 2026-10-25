package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import java.time.LocalDate;

record SourceJobDetail(
		String description,
		String seniorityLevel,
		String employmentType,
		WorkModel workModel,
		ContractType contractType,
		LocalDate postedOn,
		SalaryRange salary) {

	SourceJobDetail {
		salary = salary == null ? SalaryRange.NONE : salary;
	}

	SourceJobDetail(String description, String seniorityLevel, String employmentType) {
		this(description, seniorityLevel, employmentType, null, null, null, null);
	}

	SourceJobDetail(String description, String seniorityLevel, String employmentType, WorkModel workModel,
			ContractType contractType, LocalDate postedOn) {
		this(description, seniorityLevel, employmentType, workModel, contractType, postedOn, null);
	}
}
