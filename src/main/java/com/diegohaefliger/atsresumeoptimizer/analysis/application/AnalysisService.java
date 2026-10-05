package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.job.JobDetails;
import java.util.UUID;

public interface AnalysisService {

	AnalysisCreation create(
			ResumeUpload resumeUpload, byte[] content, String jobDescription, String targetRole, JobDetails jobDetails);

	/** Vaga vem de {@code jobPostingId}; se nulo, de {@code jobDescription}; sem nenhum dos dois vale {@code targetRole}. */
	AnalysisCreation createFromExistingVersion(UUID resumeId, UUID resumeVersionId, UUID jobPostingId,
			String jobDescription, String targetRole, JobDetails jobDetails);
}
