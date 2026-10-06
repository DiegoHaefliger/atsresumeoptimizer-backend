package com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class GoogleApiException extends BusinessException {

	public GoogleApiException(String message, Throwable cause) {
		super(message);
		initCause(cause);
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_GATEWAY;
	}
}
