package com.diegohaefliger.atsresumeoptimizer.ai;

/** A ordem de declaração é a ordem das seções no currículo final. */
public enum ResumeSectionSemanticType {
	SUMMARY,
	EXPERIENCE,
	PROJECTS,
	EDUCATION,
	SKILLS,
	CERTIFICATIONS,
	LANGUAGES,
	OTHER;

	public boolean holdsBullets() {
		return this == EXPERIENCE || this == PROJECTS;
	}
}
