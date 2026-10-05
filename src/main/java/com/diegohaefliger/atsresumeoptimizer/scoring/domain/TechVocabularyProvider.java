package com.diegohaefliger.atsresumeoptimizer.scoring.domain;

import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;

public interface TechVocabularyProvider {

	/** {@code tech_term} mais as keywords da vaga e seus equivalentes; {@code job} pode ser nulo. */
	TechVocabulary forJob(JobStructured job);
}
