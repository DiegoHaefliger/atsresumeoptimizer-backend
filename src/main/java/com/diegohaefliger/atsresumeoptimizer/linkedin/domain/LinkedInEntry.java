package com.diegohaefliger.atsresumeoptimizer.linkedin.domain;

public record LinkedInEntry(String title, String organization, String period, String location, String description) {

	public LinkedInEntry {
		title = stripped(title);
		organization = stripped(organization);
		period = stripped(period);
		location = stripped(location);
		description = stripped(description);
	}

	static String stripped(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}
}
