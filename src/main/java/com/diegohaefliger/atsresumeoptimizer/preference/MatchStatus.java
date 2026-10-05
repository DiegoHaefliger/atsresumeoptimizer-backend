package com.diegohaefliger.atsresumeoptimizer.preference;

public enum MatchStatus {
	MATCH(1.0),
	PARTIAL(0.5),
	UNKNOWN(0.5),
	MISMATCH(0.0);

	private final double fulfillment;

	MatchStatus(double fulfillment) {
		this.fulfillment = fulfillment;
	}

	public double fulfillment() {
		return fulfillment;
	}
}
