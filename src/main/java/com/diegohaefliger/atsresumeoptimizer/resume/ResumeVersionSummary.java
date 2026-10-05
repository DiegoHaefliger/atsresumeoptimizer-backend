package com.diegohaefliger.atsresumeoptimizer.resume;

import java.time.Instant;
import java.util.UUID;

public record ResumeVersionSummary(
		UUID id, int number, String fileName, String mimeType, long sizeBytes, Integer pageCount, Instant createdAt) {
}
