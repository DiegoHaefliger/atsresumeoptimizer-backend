package com.diegohaefliger.atsresumeoptimizer.linkedin.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidLinkedInSnapshotException extends BusinessException {

	public InvalidLinkedInSnapshotException(String message) {
		super(message);
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_REQUEST;
	}
}
