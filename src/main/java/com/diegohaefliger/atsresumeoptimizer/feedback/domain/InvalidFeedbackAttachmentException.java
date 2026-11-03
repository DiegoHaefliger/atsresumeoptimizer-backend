package com.diegohaefliger.atsresumeoptimizer.feedback.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidFeedbackAttachmentException extends BusinessException {

	public InvalidFeedbackAttachmentException(String message) {
		super(message);
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_REQUEST;
	}
}
