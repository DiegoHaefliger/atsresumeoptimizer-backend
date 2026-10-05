package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.rewrite.ComposedResume;
import java.util.UUID;

record StoredResumeDocuments(UUID resumeId, UUID docxVersionId, UUID pdfVersionId, byte[] docxBytes) {

	ComposedResume toComposed() {
		return new ComposedResume(resumeId, docxVersionId, pdfVersionId);
	}
}
