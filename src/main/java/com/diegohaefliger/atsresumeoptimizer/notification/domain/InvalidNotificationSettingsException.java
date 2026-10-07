package com.diegohaefliger.atsresumeoptimizer.notification.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidNotificationSettingsException extends BusinessException {

	public InvalidNotificationSettingsException(String message) {
		super(message);
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_REQUEST;
	}
}
