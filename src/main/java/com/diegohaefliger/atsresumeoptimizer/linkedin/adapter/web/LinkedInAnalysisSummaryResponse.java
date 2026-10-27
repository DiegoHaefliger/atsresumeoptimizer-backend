package com.diegohaefliger.atsresumeoptimizer.linkedin.adapter.web;

import java.time.Instant;
import java.util.UUID;

record LinkedInAnalysisSummaryResponse(UUID id, UUID snapshotId, Instant createdAt, int overallScore) {
}
