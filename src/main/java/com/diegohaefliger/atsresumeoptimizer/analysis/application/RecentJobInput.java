package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import java.time.Instant;
import java.util.UUID;

record RecentJobInput(UUID jobPostingId, String targetRole, String jobDescription, Instant lastUsedAt) {
}
