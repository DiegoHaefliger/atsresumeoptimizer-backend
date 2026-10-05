package com.diegohaefliger.atsresumeoptimizer.rewrite.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeContact;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;

record EditableResumeView(
		String title, ResumeTemplate template, StructuredResume content, ResumeContact contact, boolean importedFromFile) {
}
