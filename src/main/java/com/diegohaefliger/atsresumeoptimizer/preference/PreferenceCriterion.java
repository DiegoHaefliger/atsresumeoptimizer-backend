package com.diegohaefliger.atsresumeoptimizer.preference;

public enum PreferenceCriterion {
	WORK_MODEL(25),
	SALARY(25),
	CONTRACT_TYPE(15),
	BENEFITS(15),
	COMPANY(15),
	SENIORITY(10),
	LOCATION(10);

	private final int weight;

	PreferenceCriterion(int weight) {
		this.weight = weight;
	}

	public int weight() {
		return weight;
	}
}
