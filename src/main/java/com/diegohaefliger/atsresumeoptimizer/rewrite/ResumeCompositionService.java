package com.diegohaefliger.atsresumeoptimizer.rewrite;

import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;

public interface ResumeCompositionService {

	byte[] previewPdf(ResumeTemplate template, StructuredResume content, ResumeContact contact);

	ComposedResume compose(ResumeTemplate template, StructuredResume content, ResumeContact contact);
}
