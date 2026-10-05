package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;

/** Contato fica fora: vem da extração determinística do original, pra não depender da IA ecoar dado sensível. */
public record StructuredResume(String name, String headline, List<ResumeSection> sections, List<RemovedSkill> removedSkills) {

	public StructuredResume {
		sections = sections == null ? List.of() : List.copyOf(sections);
		removedSkills = removedSkills == null ? List.of() : List.copyOf(removedSkills);
	}

	public StructuredResume(String name, String headline, List<ResumeSection> sections) {
		this(name, headline, sections, List.of());
	}

	public StructuredResume withSections(List<ResumeSection> newSections) {
		return new StructuredResume(name, headline, newSections, removedSkills);
	}

	public StructuredResume withName(String newName) {
		return new StructuredResume(newName, headline, sections, removedSkills);
	}
}
