package com.diegohaefliger.atsresumeoptimizer.ai;

import java.math.BigDecimal;
import java.util.List;

/** Salário mensal em reais; {@code workModel} e {@code contractType} são códigos dos enums do módulo job. */
public record JobConditions(
		String company,
		String workModel,
		String contractType,
		BigDecimal salaryMin,
		BigDecimal salaryMax,
		List<String> benefits,
		String location) {

	public static final JobConditions NONE = new JobConditions(null, null, null, null, null, List.of(), null);

	public JobConditions {
		benefits = benefits == null ? List.of() : List.copyOf(benefits);
	}
}
