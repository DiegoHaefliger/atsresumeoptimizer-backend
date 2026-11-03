package com.diegohaefliger.atsresumeoptimizer.feedback.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.feedback")
record FeedbackProperties(
		@DefaultValue("") String url,
		@DefaultValue("") String publishableKey,
		@DefaultValue("feedback") String table,
		@DefaultValue("feedback-screenshots") String bucket,
		@DefaultValue("10s") Duration timeout,
		@DefaultValue("20") int maxPendingRetries) {

	boolean enabled() {
		return !url.isBlank() && !publishableKey.isBlank();
	}
}
