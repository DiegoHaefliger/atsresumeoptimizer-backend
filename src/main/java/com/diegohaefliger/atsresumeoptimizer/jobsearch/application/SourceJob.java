package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.time.LocalDate;

record SourceJob(String externalId, String url, String title, String company, String location, LocalDate postedOn,
		SourceJobDetail detail) {

	SourceJob(String externalId, String url, String title, String company, String location, LocalDate postedOn) {
		this(externalId, url, title, company, location, postedOn, null);
	}
}
