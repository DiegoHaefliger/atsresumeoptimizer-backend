package com.diegohaefliger.atsresumeoptimizer.preference;

import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface JobPreferenceMatchService {

	Optional<PreferenceMatch> match(JobOffer offer);

	Map<UUID, PreferenceMatch> matchAll(Collection<JobOffer> offers);
}
