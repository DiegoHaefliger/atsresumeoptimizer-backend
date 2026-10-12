package com.diegohaefliger.atsresumeoptimizer.resume.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** LGPD: expira o texto de versões antigas; análises já gravadas não dependem dele. */
@Component
class ResumeRetentionJob {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResumeRetentionJob.class);

	private final ResumeVersionRepository resumeVersionRepository;
	private final int retentionDays;

	ResumeRetentionJob(
			ResumeVersionRepository resumeVersionRepository,
			@Value("${app.resume.retention-days:90}") int retentionDays) {
		this.resumeVersionRepository = resumeVersionRepository;
		this.retentionDays = retentionDays;
	}

	@Scheduled(cron = "${app.resume.retention-cron:0 0 3 * * *}")
	@Transactional
	void expireOldVersions() {
		Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
		List<ResumeVersion> expired = resumeVersionRepository.findByCreatedAtBeforeAndFileNameNot(
				cutoff, ResumeVersion.SCRUBBED_PLACEHOLDER);
		if (expired.isEmpty()) {
			return;
		}
		expired.forEach(ResumeVersion::scrub);
		resumeVersionRepository.saveAll(expired);
		LOGGER.info("Retenção LGPD: {} resume_version expiradas (mais de {} dias)", expired.size(), retentionDays);
	}
}
