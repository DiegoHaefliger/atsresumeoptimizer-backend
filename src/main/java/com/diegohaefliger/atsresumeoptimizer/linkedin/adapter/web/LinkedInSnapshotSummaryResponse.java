package com.diegohaefliger.atsresumeoptimizer.linkedin.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.SnapshotSource;
import java.time.Instant;
import java.util.UUID;

record LinkedInSnapshotSummaryResponse(
		UUID id, Instant capturedAt, SnapshotSource source, String fullName, String headline) {
}
