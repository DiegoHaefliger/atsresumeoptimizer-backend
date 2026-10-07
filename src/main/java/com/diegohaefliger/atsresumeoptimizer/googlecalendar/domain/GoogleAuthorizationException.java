package com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class GoogleAuthorizationException extends BusinessException {

	public GoogleAuthorizationException(String message) {
		super(message);
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_REQUEST;
	}
}
