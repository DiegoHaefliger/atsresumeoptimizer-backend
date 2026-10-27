package com.diegohaefliger.atsresumeoptimizer.linkedin.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class LinkedInAnalysisUnavailableException extends BusinessException {

	public LinkedInAnalysisUnavailableException(String message) {
		super(message);
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_GATEWAY;
	}
}
