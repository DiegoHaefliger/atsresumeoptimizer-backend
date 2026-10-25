package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SearchProgress;

@Component
class SearchProgressTracker {

	private final AtomicInteger read = new AtomicInteger();
	private final AtomicInteger added = new AtomicInteger();
	private final AtomicInteger belowAdherence = new AtomicInteger();

	void reset() {
		read.set(0);
		added.set(0);
		belowAdherence.set(0);
	}

	void read() {
		read.incrementAndGet();
	}

	void added() {
		added.incrementAndGet();
	}

	void belowAdherence() {
		belowAdherence.incrementAndGet();
	}

	SearchProgress snapshot() {
		return new SearchProgress(read.get(), added.get(), belowAdherence.get());
	}
}
