package com.diegohaefliger.atsresumeoptimizer.chat.application.tool;

import com.diegohaefliger.atsresumeoptimizer.job.JobListing;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class ChatJobLookup {

	private final JobStructuringService jobService;

	ChatJobLookup(JobStructuringService jobService) {
		this.jobService = jobService;
	}

	List<JobOffer> all() {
		return jobService.listJobs().stream().map(JobListing::offer).toList();
	}

	Optional<JobOffer> byCode(String code) {
		long number;
		try {
			number = Long.parseLong(code.strip().replace("#", ""));
		} catch (NumberFormatException notANumber) {
			return Optional.empty();
		}
		return all().stream().filter(offer -> offer.code() != null && offer.code() == number).findFirst();
	}
}
