package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import java.time.Instant;
import java.util.UUID;

interface LinkedInAnalysisHead {

	UUID getId();

	UUID getSnapshotId();

	Instant getCreatedAt();

	int getOverallScore();
}
