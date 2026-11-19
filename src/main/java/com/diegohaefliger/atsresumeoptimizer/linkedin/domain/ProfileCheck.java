package com.diegohaefliger.atsresumeoptimizer.linkedin.domain;

public record ProfileCheck(String label, boolean passed, String section, String current, String action) {

	public static ProfileCheck ok(String label) {
		return new ProfileCheck(label, true, null, null, null);
	}

	public static ProfileCheck failed(String label, String section, String current, String action) {
		return new ProfileCheck(label, false, section, current, action);
	}
}
