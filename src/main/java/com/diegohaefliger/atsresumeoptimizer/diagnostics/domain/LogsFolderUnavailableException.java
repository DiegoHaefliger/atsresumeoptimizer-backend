package com.diegohaefliger.atsresumeoptimizer.diagnostics.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class LogsFolderUnavailableException extends BusinessException {

	public LogsFolderUnavailableException(String message) {
		super(message);
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.CONFLICT;
	}
}
