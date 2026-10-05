package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisSnapshot;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import org.springframework.util.StringUtils;

final class AdaptedResumeTitle {

	private static final String ADAPTED_FOR = "adaptado para ";
	private static final String ADAPTED_FOR_JOB = "adaptado para a vaga";
	private static final String ADAPTED_GENERAL = "adaptado na avaliação geral";

	private AdaptedResumeTitle() {
	}

	static String suffix(JobStructured job, AnalysisSnapshot snapshot) {
		if (job != null && StringUtils.hasText(job.title())) {
			return ADAPTED_FOR + job.title();
		}
		if (StringUtils.hasText(snapshot.targetRole())) {
			return ADAPTED_FOR + snapshot.targetRole();
		}
		return snapshot.mode() == AnalysisMode.JOB_MATCH ? ADAPTED_FOR_JOB : ADAPTED_GENERAL;
	}
}
