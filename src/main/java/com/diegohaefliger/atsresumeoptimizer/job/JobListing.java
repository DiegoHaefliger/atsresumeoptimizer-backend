package com.diegohaefliger.atsresumeoptimizer.job;

import java.time.Instant;

public record JobListing(JobOffer offer, Instant registeredAt, JobOrigin origin) {

	public JobListing(JobOffer offer, Instant registeredAt) {
		this(offer, registeredAt, JobOrigin.MANUAL);
	}
}
