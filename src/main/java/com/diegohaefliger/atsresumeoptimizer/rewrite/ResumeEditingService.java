package com.diegohaefliger.atsresumeoptimizer.rewrite;

import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import java.util.UUID;

public interface ResumeEditingService {

	EditableResume load(UUID resumeId, UUID resumeVersionId);

	ExportedResume export(UUID resumeId, UUID resumeVersionId, ResumeFormat format);

	/** A edição vira versão nova (DOCX + PDF) do mesmo currículo; a versão editada continua no histórico. */
	ComposedResume save(UUID resumeId, UUID resumeVersionId, String title, ResumeTemplate template, StructuredResume content,
			ResumeContact contact);
}
