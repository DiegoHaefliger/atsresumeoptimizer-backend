package com.diegohaefliger.atsresumeoptimizer.jobsearch.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class SearchAlreadyRunningException extends BusinessException {

	public SearchAlreadyRunningException() {
		super("Já tem uma busca em andamento.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.CONFLICT;
	}
}
