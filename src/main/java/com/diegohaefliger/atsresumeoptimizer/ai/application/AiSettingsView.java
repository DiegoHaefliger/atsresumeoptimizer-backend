package com.diegohaefliger.atsresumeoptimizer.ai.application;

import java.math.BigDecimal;
import java.util.List;

public record AiSettingsView(
		BigDecimal temperature,
		int maxOutputTokens,
		int timeoutSeconds,
		boolean encryptionAvailable,
		List<AiProviderView> providers) {
}
