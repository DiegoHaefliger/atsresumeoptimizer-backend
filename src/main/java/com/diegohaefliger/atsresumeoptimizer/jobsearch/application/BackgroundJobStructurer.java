package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.diegohaefliger.atsresumeoptimizer.job.JobDetails;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import jakarta.annotation.PreDestroy;

@Component
class BackgroundJobStructurer {

	private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundJobStructurer.class);

	private final JobStructuringService jobService;
	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

	BackgroundJobStructurer(JobStructuringService jobService) {
		this.jobService = jobService;
	}

	void structureAfterCommit(String jobText, JobDetails details) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			structure(jobText, details);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				structure(jobText, details);
			}
		});
	}

	private void structure(String jobText, JobDetails details) {
		executor.execute(() -> {
			try {
				jobService.structureFromText(jobText, details);
			} catch (RuntimeException exception) {
				LOGGER.warn("Vaga promovida ficou sem estruturação: {}", exception.getMessage());
			}
		});
	}

	@PreDestroy
	void shutdown() {
		executor.shutdownNow();
	}
}
