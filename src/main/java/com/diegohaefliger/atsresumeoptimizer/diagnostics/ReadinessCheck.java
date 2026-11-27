package com.diegohaefliger.atsresumeoptimizer.diagnostics;

public record ReadinessCheck(String name, ReadinessStatus status, String detail, String hint) {

	public static ReadinessCheck ok(String name, String detail) {
		return new ReadinessCheck(name, ReadinessStatus.OK, detail, null);
	}

	public static ReadinessCheck warning(String name, String detail, String hint) {
		return new ReadinessCheck(name, ReadinessStatus.WARNING, detail, hint);
	}

	public static ReadinessCheck failure(String name, String detail, String hint) {
		return new ReadinessCheck(name, ReadinessStatus.FAILURE, detail, hint);
	}
}
