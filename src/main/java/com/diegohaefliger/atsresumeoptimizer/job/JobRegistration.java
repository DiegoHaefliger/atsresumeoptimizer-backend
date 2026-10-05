package com.diegohaefliger.atsresumeoptimizer.job;

import java.util.List;
import java.util.UUID;

public record JobRegistration(UUID id, String title, String seniority, List<String> requiredKeywords) {

	public JobRegistration {
		requiredKeywords = requiredKeywords == null ? List.of() : List.copyOf(requiredKeywords);
	}
}
