package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeSummary;
import java.util.List;
import java.util.UUID;

public interface AnalysisQueryService {

	AnalysisReportView get(AnalysisId id);

	AnalysisAtsView getAtsView(AnalysisId id);

	/** Currículos adaptados nas análises feitas para a vaga, do mais novo para o mais antigo. */
	List<ResumeSummary> generatedResumes(UUID jobPostingId);
}
