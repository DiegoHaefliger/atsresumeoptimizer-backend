package com.diegohaefliger.atsresumeoptimizer.job;

import java.util.Arrays;
import java.util.Optional;

public enum ContractType {
	CLT("CLT"),
	PJ("PJ"),
	INTERNSHIP("Estágio"),
	TEMPORARY("Temporário");

	private final String label;

	ContractType(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public static Optional<ContractType> fromCode(String code) {
		return Arrays.stream(values()).filter(value -> value.name().equalsIgnoreCase(code)).findFirst();
	}
}
