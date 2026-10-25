package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.util.List;
import java.util.Optional;

interface JobSource {

	String name();

	String label();

	int pageSize();

	List<SourceJob> search(SourceQuery query);

	Optional<SourceJobDetail> detail(String externalId);

	default Optional<String> externalIdOf(String url) {
		return Optional.empty();
	}
}
