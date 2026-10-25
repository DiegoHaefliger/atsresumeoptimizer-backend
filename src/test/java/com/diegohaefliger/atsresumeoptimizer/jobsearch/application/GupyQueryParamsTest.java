package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.preference.PostedWithin;

class GupyQueryParamsTest {

	@Test
	void mapsTheCriteriaToGupyFilters() {
		var query = new SourceQuery("desenvolvedor java", "Curitiba, PR", PostedWithin.PAST_WEEK,
				List.of(WorkModel.REMOTE, WorkModel.HYBRID), List.of("Pleno"), List.of(ContractType.CLT, ContractType.PJ), 0);

		assertThat(GupyQueryParams.of(query)).isEqualTo("term=desenvolvedor%20java&city=Curitiba"
				+ "&workplaceTypes%5B%5D=remote&workplaceTypes%5B%5D=hybrid"
				+ "&jobTypes%5B%5D=vacancy_type_effective&jobTypes%5B%5D=vacancy_legal_entity");
	}

	@Test
	void doesNotFilterByCityWhenTheSearchIsNationwide() {
		var query = new SourceQuery("java", "Brazil", PostedWithin.ANY, List.of(), List.of(), List.of(), 0);

		assertThat(GupyQueryParams.of(query)).isEqualTo("term=java");
	}
}
