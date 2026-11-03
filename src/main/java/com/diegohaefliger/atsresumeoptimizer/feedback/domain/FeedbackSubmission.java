package com.diegohaefliger.atsresumeoptimizer.feedback.domain;

public record FeedbackSubmission(
		int rating,
		String message,
		String contact,
		boolean allowPublish,
		String appVersion) {
}
