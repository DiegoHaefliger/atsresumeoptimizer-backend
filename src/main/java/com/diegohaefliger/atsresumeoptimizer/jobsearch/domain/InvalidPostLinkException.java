package com.diegohaefliger.atsresumeoptimizer.jobsearch.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidPostLinkException extends BusinessException {

	public InvalidPostLinkException() {
		super("Informe o link de uma postagem do LinkedIn.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_REQUEST;
	}
}
