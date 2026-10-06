package com.diegohaefliger.atsresumeoptimizer.selection.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidScheduleException extends BusinessException {

	public InvalidScheduleException(String message) {
		super(message);
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_REQUEST;
	}
}
