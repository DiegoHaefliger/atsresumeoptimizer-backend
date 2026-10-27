package com.diegohaefliger.atsresumeoptimizer.linkedin.domain;

import java.time.Instant;
import java.util.UUID;

public record LinkedInAnalysisSummary(UUID id, UUID snapshotId, Instant createdAt, int overallScore) {
}
