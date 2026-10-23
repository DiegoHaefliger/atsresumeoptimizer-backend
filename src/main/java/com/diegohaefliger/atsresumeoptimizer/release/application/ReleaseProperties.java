package com.diegohaefliger.atsresumeoptimizer.release.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.update-check")
record ReleaseProperties(
		@DefaultValue("true") boolean enabled,
		@DefaultValue("DiegoHaefliger/atsresumeoptimizer") String repository,
		@DefaultValue("https://api.github.com") String apiUrl,
		@DefaultValue("1h") Duration cacheTtl,
		@DefaultValue("30m") Duration failureRetry,
		@DefaultValue("5s") Duration timeout) {

	String owner() {
		return repository.substring(0, repository.indexOf('/'));
	}

	String name() {
		return repository.substring(repository.indexOf('/') + 1);
	}
}
