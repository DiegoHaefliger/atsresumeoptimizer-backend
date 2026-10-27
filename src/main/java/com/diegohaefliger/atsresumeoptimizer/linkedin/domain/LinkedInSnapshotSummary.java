package com.diegohaefliger.atsresumeoptimizer.linkedin.domain;

import java.time.Instant;
import java.util.UUID;

public record LinkedInSnapshotSummary(
		UUID id, Instant capturedAt, SnapshotSource source, String fullName, String headline) {
}
