package com.diegohaefliger.atsresumeoptimizer.preference.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import com.diegohaefliger.atsresumeoptimizer.preference.JobSearchCriteria;
import com.diegohaefliger.atsresumeoptimizer.preference.PostedWithin;
import com.diegohaefliger.atsresumeoptimizer.preference.domain.JobPreference;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobSearchCriteriaProviderImplTest {

	@Mock
	private JobPreferenceService preferenceService;

	@Test
	void reusesThePreferenceFieldsAndAddsTheSearchOnes() {
		when(preferenceService.current()).thenReturn(new JobPreference(List.of(WorkModel.REMOTE), List.of(), null, null,
				List.of(), List.of("Pleno"), List.of("Curitiba"), List.of(), List.of(), List.of("Java"), List.of("estágio"),
				PostedWithin.PAST_MONTH, 30, 80, true, 6));

		JobSearchCriteria criteria = new JobSearchCriteriaProviderImpl(preferenceService).current();

		assertThat(criteria.keywords()).containsExactly("Java");
		assertThat(criteria.seniorities()).containsExactly("Pleno");
		assertThat(criteria.locations()).containsExactly("Curitiba");
		assertThat(criteria.workModels()).containsExactly(WorkModel.REMOTE);
		assertThat(criteria.postedWithin()).isEqualTo(PostedWithin.PAST_MONTH);
		assertThat(criteria.minAdherence()).isEqualTo(80);
		assertThat(criteria.interval()).isEqualTo(Duration.ofHours(6));
		assertThat(criteria.enabled()).isTrue();
	}
}
