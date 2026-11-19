package com.diegohaefliger.atsresumeoptimizer.linkedin.domain;

public record ProfileCheck(String label, boolean passed, String detail) {

	public static ProfileCheck ok(String label) {
		return new ProfileCheck(label, true, null);
	}

	public static ProfileCheck failed(String label, String detail) {
		return new ProfileCheck(label, false, detail);
	}
}
