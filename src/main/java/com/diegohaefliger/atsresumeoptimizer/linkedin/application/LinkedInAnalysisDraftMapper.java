package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.ai.LinkedInAnalysisDraft;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysisReport;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.ProfileSection;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.SuggestionPriority;
import org.mapstruct.Mapper;

@Mapper
interface LinkedInAnalysisDraftMapper {

	int MAX_SCORE = 100;

	LinkedInAnalysisReport toReport(LinkedInAnalysisDraft draft);

	default int score(Integer value) {
		return value == null ? 0 : Math.max(0, Math.min(MAX_SCORE, value));
	}

	default ProfileSection section(String code) {
		try {
			return code == null ? null : ProfileSection.valueOf(code.strip().toUpperCase());
		} catch (IllegalArgumentException unknown) {
			return null;
		}
	}

	default SuggestionPriority priority(String code) {
		try {
			return code == null ? SuggestionPriority.MEDIUM : SuggestionPriority.valueOf(code.strip().toUpperCase());
		} catch (IllegalArgumentException unknown) {
			return SuggestionPriority.MEDIUM;
		}
	}
}
