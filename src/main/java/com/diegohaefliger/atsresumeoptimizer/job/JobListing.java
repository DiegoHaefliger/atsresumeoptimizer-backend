package com.diegohaefliger.atsresumeoptimizer.job;

import java.time.Instant;

public record JobListing(JobOffer offer, Instant registeredAt) {
}
