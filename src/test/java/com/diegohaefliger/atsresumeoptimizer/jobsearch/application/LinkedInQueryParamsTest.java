package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.preference.PostedWithin;
import org.junit.jupiter.api.Test;

class LinkedInQueryParamsTest {

	@Test
	void mapsEveryCriterionToItsLinkedInFilter() {
		var query = new SourceQuery("java", "Curitiba", PostedWithin.PAST_24_HOURS,
				List.of(WorkModel.REMOTE, WorkModel.HYBRID), List.of("Júnior", "Sênior", "Tech Lead"),
				List.of(ContractType.CLT, ContractType.PJ), 20);

		assertThat(LinkedInQueryParams.of(query)).containsEntry("keywords", "java").containsEntry("location", "Curitiba")
				.containsEntry("f_TPR", "r86400").containsEntry("f_WT", "2,3").containsEntry("f_JT", "F,C")
				.containsEntry("f_E", "2,4").containsEntry("start", "20");
	}

	@Test
	void leavesOutTheFiltersThatWereNotChosen() {
		var query = new SourceQuery("java", "Brazil", PostedWithin.ANY, List.of(), List.of(), List.of(), 0);

		assertThat(LinkedInQueryParams.of(query)).containsOnlyKeys("keywords", "location", "start");
	}
}
