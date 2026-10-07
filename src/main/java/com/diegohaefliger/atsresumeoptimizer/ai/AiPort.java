package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;

public interface AiPort {

	AiResult<JobStructured> structureJob(String jobText);

	AiResult<List<BulletReview>> reviewBullets(List<String> bullets);

	AiResult<StructuredResume> structureResume(String resumeText, String correctionInstructions, JobFocus jobFocus);

	AiResult<List<RequirementEvidence>> findRequirementEvidence(String resumeText, List<String> requirements);

	/** Sem cache: gerar de novo tem que trazer outro texto. */
	AiResult<CoverLetterDraft> writeCoverLetter(CoverLetterBrief brief);
}
