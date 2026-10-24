package com.diegohaefliger.atsresumeoptimizer.preference.application;

import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.preference.PreferenceAdherenceFilter;
import com.diegohaefliger.atsresumeoptimizer.preference.PreferenceMatch;
import com.diegohaefliger.atsresumeoptimizer.preference.domain.JobPreference;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
class PreferenceAdherenceFilterImpl implements PreferenceAdherenceFilter {

	private final JobPreferenceService preferenceService;
	private final PreferenceMatcher matcher;

	PreferenceAdherenceFilterImpl(JobPreferenceService preferenceService, PreferenceMatcher matcher) {
		this.preferenceService = preferenceService;
		this.matcher = matcher;
	}

	@Override
	public List<JobOffer> keepAdherent(Collection<JobOffer> offers) {
		JobPreference preference = preferenceService.current();
		return offers.stream().filter(offer -> reaches(preference, matcher.match(preference, offer))).toList();
	}

	private boolean reaches(JobPreference preference, Optional<PreferenceMatch> match) {
		return match.map(PreferenceMatch::score).filter(score -> score >= preference.searchMinAdherence()).isPresent();
	}
}
