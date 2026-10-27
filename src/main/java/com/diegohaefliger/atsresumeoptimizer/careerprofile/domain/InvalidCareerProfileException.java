package com.diegohaefliger.atsresumeoptimizer.careerprofile.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidCareerProfileException extends BusinessException {

	public InvalidCareerProfileException(String message) {
		super(message);
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_REQUEST;
	}
}
