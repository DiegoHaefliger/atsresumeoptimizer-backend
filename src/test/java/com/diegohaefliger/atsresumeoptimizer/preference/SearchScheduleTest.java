package com.diegohaefliger.atsresumeoptimizer.preference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.diegohaefliger.atsresumeoptimizer.preference.domain.InvalidJobPreferenceException;

class SearchScheduleTest {

	@ParameterizedTest
	@ValueSource(strings = { "0 0 8,20 * * *", "0 0 */6 * * *", "0 0 * * * *", "0 30 9 * * MON-FRI", "0 0 8 * * *" })
	void acceptsSchedulesWithAtLeastOneHourBetweenRuns(String cron) {
		assertThat(SearchSchedule.parse(cron)).isNotNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "0 */30 * * * *", "0 * * * * *", "* * * * * *", "0 0,30 * * * *" })
	void rejectsSchedulesThatRunMoreOftenThanHourly(String cron) {
		assertThatThrownBy(() -> SearchSchedule.parse(cron)).isInstanceOf(InvalidJobPreferenceException.class)
				.hasMessageContaining("1 hora");
	}

	@ParameterizedTest
	@ValueSource(strings = { "toda hora", "", "0 0 8 * *", "0 0 25 * * *" })
	void rejectsInvalidExpressions(String cron) {
		assertThatThrownBy(() -> SearchSchedule.parse(cron)).isInstanceOf(InvalidJobPreferenceException.class);
	}

	@Test
	void rejectsNull() {
		assertThatThrownBy(() -> SearchSchedule.parse(null)).isInstanceOf(InvalidJobPreferenceException.class);
	}
}
