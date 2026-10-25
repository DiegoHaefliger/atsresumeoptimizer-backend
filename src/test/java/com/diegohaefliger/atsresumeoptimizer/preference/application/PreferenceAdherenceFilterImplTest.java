package com.diegohaefliger.atsresumeoptimizer.preference.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.preference.CriterionMatch;
import com.diegohaefliger.atsresumeoptimizer.preference.MatchStatus;
import com.diegohaefliger.atsresumeoptimizer.preference.PreferenceCriterion;
import com.diegohaefliger.atsresumeoptimizer.preference.PreferenceMatch;
import com.diegohaefliger.atsresumeoptimizer.preference.domain.JobPreference;
import com.diegohaefliger.atsresumeoptimizer.preference.PostedWithin;
import com.diegohaefliger.atsresumeoptimizer.preference.SearchSite;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreferenceAdherenceFilterImplTest {

	@Mock
	private JobPreferenceService preferenceService;

	@Mock
	private PreferenceMatcher matcher;

	private final JobPreference preference = new JobPreference(List.of(), List.of(), null, null, List.of(), List.of(),
			List.of(), List.of(), List.of(), List.of("Java"), List.of(), PostedWithin.PAST_WEEK, 50, 70, true, "0 0 */12 * * *", List.of());

	private static JobOffer offer() {
		return new JobOffer(UUID.randomUUID(), null, "Dev", null, null, null, null, null, null, null, List.of(), null,
				null, "texto");
	}

	private static Optional<PreferenceMatch> match(Integer score) {
		var criterion = new CriterionMatch(PreferenceCriterion.WORK_MODEL, MatchStatus.MATCH, "");
		return Optional.of(new PreferenceMatch(score, List.of(criterion)));
	}

	@Test
	void keepsOnlyOffersWithInformationAndScoreAtOrAboveTheMinimum() {
		var above = offer();
		var exact = offer();
		var below = offer();
		var noScore = offer();
		var noInformation = offer();
		when(preferenceService.current()).thenReturn(preference);
		when(matcher.match(preference, above)).thenReturn(match(85));
		when(matcher.match(preference, exact)).thenReturn(match(70));
		when(matcher.match(preference, below)).thenReturn(match(69));
		when(matcher.match(preference, noScore)).thenReturn(match(null));
		when(matcher.match(preference, noInformation)).thenReturn(Optional.empty());

		var kept = new PreferenceAdherenceFilterImpl(preferenceService, matcher)
				.keepAdherent(List.of(above, exact, below, noScore, noInformation));

		assertThat(kept).containsExactly(above, exact);
	}
}
