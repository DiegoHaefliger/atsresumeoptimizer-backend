package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
class RequestPacer {

	private final JobSearchProperties properties;

	RequestPacer(JobSearchProperties properties) {
		this.properties = properties;
	}

	void pause() {
		long min = properties.minDelay().toMillis();
		long max = Math.max(min, properties.maxDelay().toMillis());
		long millis = min == max ? min : ThreadLocalRandom.current().nextLong(min, max + 1);
		if (millis <= 0) {
			return;
		}
		try {
			Thread.sleep(millis);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new JobSourceException("Busca interrompida.", exception);
		}
	}
}
