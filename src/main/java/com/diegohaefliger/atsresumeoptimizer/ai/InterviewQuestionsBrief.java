package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;

public record InterviewQuestionsBrief(
		String jobTitle,
		String company,
		String seniority,
		String jobText,
		List<String> priorityKeywords,
		List<String> requirements,
		String resumeText) {

	public InterviewQuestionsBrief {
		priorityKeywords = priorityKeywords == null ? List.of() : List.copyOf(priorityKeywords);
		requirements = requirements == null ? List.of() : List.copyOf(requirements);
	}
}
