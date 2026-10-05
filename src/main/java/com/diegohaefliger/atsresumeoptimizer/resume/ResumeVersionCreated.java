package com.diegohaefliger.atsresumeoptimizer.resume;

import java.util.UUID;

public record ResumeVersionCreated(
		UUID resumeId, UUID resumeVersionId, String storageKey, long sizeBytes, String sha256, Integer pageCount) {
}
