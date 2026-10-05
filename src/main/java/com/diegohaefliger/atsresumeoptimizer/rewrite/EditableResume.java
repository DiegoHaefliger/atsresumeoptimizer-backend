package com.diegohaefliger.atsresumeoptimizer.rewrite;

import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;

/** {@code importedFromFile}: o conteúdo saiu do texto do arquivo enviado, não de um currículo montado no editor. */
public record EditableResume(String title, ResumeTemplate template, StructuredResume content, ResumeContact contact,
		boolean importedFromFile) {

	public EditableResume withTitle(String newTitle) {
		return new EditableResume(newTitle, template, content, contact, importedFromFile);
	}
}
