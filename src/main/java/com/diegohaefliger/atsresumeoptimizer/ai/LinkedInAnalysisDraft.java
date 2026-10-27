package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;

public record LinkedInAnalysisDraft(
		Integer overallScore,
		String summary,
		String targetFit,
		List<String> topActions,
		List<String> recruiterKeywords,
		List<String> missingKeywords,
		List<LinkedInSectionReviewDraft> sections) {

	public LinkedInAnalysisDraft {
		topActions = topActions == null ? List.of() : List.copyOf(topActions);
		recruiterKeywords = recruiterKeywords == null ? List.of() : List.copyOf(recruiterKeywords);
		missingKeywords = missingKeywords == null ? List.of() : List.copyOf(missingKeywords);
		sections = sections == null ? List.of() : List.copyOf(sections);
	}
}
