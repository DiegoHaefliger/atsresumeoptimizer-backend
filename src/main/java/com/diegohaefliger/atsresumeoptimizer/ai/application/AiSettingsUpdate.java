package com.diegohaefliger.atsresumeoptimizer.ai.application;

import java.math.BigDecimal;
import java.util.List;

/** A ordem de {@code providers} é a prioridade de execução entre os habilitados. */
public record AiSettingsUpdate(
		BigDecimal temperature,
		int maxOutputTokens,
		int timeoutSeconds,
		List<AiProviderUpdate> providers) {

	public AiSettingsUpdate {
		providers = providers == null ? List.of() : List.copyOf(providers);
	}
}
