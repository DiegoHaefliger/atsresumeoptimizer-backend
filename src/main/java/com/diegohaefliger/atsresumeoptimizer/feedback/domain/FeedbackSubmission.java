package com.diegohaefliger.atsresumeoptimizer.feedback.domain;

public record FeedbackSubmission(
		int rating,
		String message,
		String contact,
		String appVersion) {
}
