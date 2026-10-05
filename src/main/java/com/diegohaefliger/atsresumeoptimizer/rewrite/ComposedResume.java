package com.diegohaefliger.atsresumeoptimizer.rewrite;

import java.util.UUID;

public record ComposedResume(UUID resumeId, UUID docxResumeVersionId, UUID pdfResumeVersionId) {
}
