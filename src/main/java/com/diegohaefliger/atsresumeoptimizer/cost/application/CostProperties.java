package com.diegohaefliger.atsresumeoptimizer.cost.application;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cost")
public record CostProperties(BigDecimal usdBrlRate) {

	private static final BigDecimal DEFAULT_USD_BRL_RATE = new BigDecimal("5.50");

	public CostProperties {
		if (usdBrlRate == null || usdBrlRate.signum() <= 0) {
			usdBrlRate = DEFAULT_USD_BRL_RATE;
		}
	}
}
