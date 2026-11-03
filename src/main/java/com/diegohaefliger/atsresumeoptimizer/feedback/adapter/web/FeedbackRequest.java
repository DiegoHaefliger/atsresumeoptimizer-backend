package com.diegohaefliger.atsresumeoptimizer.feedback.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackKind;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record FeedbackRequest(
		@NotNull FeedbackKind kind,
		@NotNull @Min(FeedbackRequest.MIN_RATING) @Max(FeedbackRequest.MAX_RATING) Integer rating,
		@Size(max = FeedbackRequest.MAX_MESSAGE) String message,
		@Email @Size(max = FeedbackRequest.MAX_CONTACT) String contact,
		Boolean allowPublish,
		@Size(max = FeedbackRequest.MAX_VERSION) String appVersion) {

	static final int MIN_RATING = 1;
	static final int MAX_RATING = 5;
	static final int MAX_MESSAGE = 4000;
	static final int MAX_CONTACT = 200;
	static final int MAX_VERSION = 40;
}
