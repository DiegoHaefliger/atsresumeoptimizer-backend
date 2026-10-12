package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;

public record CandidateQuestionsBrief(
		String jobTitle, String company, String seniority, String jobText, List<String> benefits) {

	public CandidateQuestionsBrief {
		benefits = benefits == null ? List.of() : List.copyOf(benefits);
	}
}
