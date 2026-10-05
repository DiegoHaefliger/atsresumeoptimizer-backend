package com.diegohaefliger.atsresumeoptimizer.job;

import java.util.Arrays;
import java.util.Optional;

public enum WorkModel {
	REMOTE("Remoto"),
	HYBRID("Híbrido"),
	ON_SITE("Presencial");

	private final String label;

	WorkModel(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public static Optional<WorkModel> fromCode(String code) {
		return Arrays.stream(values()).filter(value -> value.name().equalsIgnoreCase(code)).findFirst();
	}
}
