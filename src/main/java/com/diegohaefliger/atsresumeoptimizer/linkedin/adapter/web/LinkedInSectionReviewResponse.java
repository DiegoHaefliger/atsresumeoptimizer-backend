package com.diegohaefliger.atsresumeoptimizer.linkedin.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.ProfileSection;
import java.util.List;

record LinkedInSectionReviewResponse(
		ProfileSection section,
		int score,
		String diagnosis,
		List<LinkedInSuggestionResponse> suggestions,
		String rewrite,
		List<LinkedInItemReviewResponse> items) {
}
