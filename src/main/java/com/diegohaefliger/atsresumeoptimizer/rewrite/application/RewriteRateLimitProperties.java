package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rewrite.rate-limit")
public record RewriteRateLimitProperties(int capacity, Duration period) {

	public RewriteRateLimitProperties {
		if (capacity <= 0) {
			capacity = 5;
		}
		if (period == null) {
			period = Duration.ofDays(1);
		}
	}
}
