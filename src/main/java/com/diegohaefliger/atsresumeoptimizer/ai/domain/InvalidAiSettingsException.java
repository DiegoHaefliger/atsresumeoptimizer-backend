package com.diegohaefliger.atsresumeoptimizer.ai.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidAiSettingsException extends BusinessException {

	public InvalidAiSettingsException(String message) {
		super(message);
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_REQUEST;
	}
}
