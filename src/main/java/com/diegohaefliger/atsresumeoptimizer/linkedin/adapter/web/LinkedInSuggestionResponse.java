package com.diegohaefliger.atsresumeoptimizer.linkedin.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.SuggestionPriority;

record LinkedInSuggestionResponse(SuggestionPriority priority, String text) {
}
