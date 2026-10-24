package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.job-search")
record JobSearchProperties(
		@DefaultValue("https://www.linkedin.com") String baseUrl,
		@DefaultValue("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36") String userAgent,
		@DefaultValue("2s") Duration minDelay,
		@DefaultValue("5s") Duration maxDelay,
		@DefaultValue("15s") Duration timeout,
		@DefaultValue("Brazil") String defaultLocation) {
}
