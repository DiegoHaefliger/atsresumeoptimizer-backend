package com.diegohaefliger.atsresumeoptimizer.preference.application;

import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.preference.JobPreferenceMatchService;
import com.diegohaefliger.atsresumeoptimizer.preference.PreferenceMatch;
import com.diegohaefliger.atsresumeoptimizer.preference.domain.JobPreference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class JobPreferenceMatchServiceImpl implements JobPreferenceMatchService {

	private final JobPreferenceService preferenceService;
	private final PreferenceMatcher matcher;

	JobPreferenceMatchServiceImpl(JobPreferenceService preferenceService, PreferenceMatcher matcher) {
		this.preferenceService = preferenceService;
		this.matcher = matcher;
	}

	@Override
	public Optional<PreferenceMatch> match(JobOffer offer) {
		return matcher.match(preferenceService.current(), offer);
	}

	@Override
	public Map<UUID, PreferenceMatch> matchAll(Collection<JobOffer> offers) {
		JobPreference preference = preferenceService.current();
		Map<UUID, PreferenceMatch> matches = new HashMap<>();
		for (JobOffer offer : offers) {
			matcher.match(preference, offer).ifPresent(match -> matches.put(offer.jobPostingId(), match));
		}
		return Map.copyOf(matches);
	}
}
