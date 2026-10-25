package com.diegohaefliger.atsresumeoptimizer.preference;

import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import java.util.Collection;
import java.util.List;

public interface PreferenceAdherenceFilter {

	List<JobOffer> keepAdherent(Collection<JobOffer> offers);
}
