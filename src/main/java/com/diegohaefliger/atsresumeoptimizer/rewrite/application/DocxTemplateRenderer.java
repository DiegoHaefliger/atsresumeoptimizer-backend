package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import java.util.List;

interface DocxTemplateRenderer {

	byte[] render(ResumeTheme theme, StructuredResume content, List<ContactField> contact);
}
