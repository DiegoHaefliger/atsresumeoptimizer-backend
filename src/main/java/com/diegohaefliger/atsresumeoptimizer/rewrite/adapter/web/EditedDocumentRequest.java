package com.diegohaefliger.atsresumeoptimizer.rewrite.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeContact;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record EditedDocumentRequest(@Size(max = 255) String title, @NotNull ResumeTemplate template, @NotNull StructuredResume content,
		@NotNull ResumeContact contact) {
}
