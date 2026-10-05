package com.diegohaefliger.atsresumeoptimizer.preference.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidJobPreferenceException extends BusinessException {

	public InvalidJobPreferenceException(String message) {
		super(message);
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_REQUEST;
	}
}
