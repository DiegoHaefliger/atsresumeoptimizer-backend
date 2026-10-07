package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;

public record CoverLetterBrief(
		String jobTitle, String company, String jobText, List<String> priorityKeywords, String resumeText) {

	public CoverLetterBrief {
		priorityKeywords = priorityKeywords == null ? List.of() : List.copyOf(priorityKeywords);
	}
}
