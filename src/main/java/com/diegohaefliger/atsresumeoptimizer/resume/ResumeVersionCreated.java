package com.diegohaefliger.atsresumeoptimizer.resume;

import java.util.UUID;

public record ResumeVersionCreated(
		UUID resumeId, UUID resumeVersionId, long sizeBytes, String sha256, Integer pageCount) {
}
