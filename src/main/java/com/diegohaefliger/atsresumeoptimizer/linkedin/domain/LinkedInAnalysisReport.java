package com.diegohaefliger.atsresumeoptimizer.linkedin.domain;

import java.util.List;

public record LinkedInAnalysisReport(
		String targetFit,
		List<String> topActions,
		List<String> recruiterKeywords,
		List<String> missingKeywords,
		List<LinkedInSectionReview> sections) {

	public LinkedInAnalysisReport {
		topActions = topActions == null ? List.of() : List.copyOf(topActions);
		recruiterKeywords = recruiterKeywords == null ? List.of() : List.copyOf(recruiterKeywords);
		missingKeywords = missingKeywords == null ? List.of() : List.copyOf(missingKeywords);
		sections = sections == null ? List.of() : List.copyOf(sections);
	}
}
