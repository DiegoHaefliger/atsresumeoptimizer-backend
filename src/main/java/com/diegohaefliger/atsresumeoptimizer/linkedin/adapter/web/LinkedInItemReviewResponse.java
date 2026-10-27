package com.diegohaefliger.atsresumeoptimizer.linkedin.adapter.web;

import java.util.List;

record LinkedInItemReviewResponse(
		String title, int score, String diagnosis, List<LinkedInSuggestionResponse> suggestions, String rewrite) {
}
