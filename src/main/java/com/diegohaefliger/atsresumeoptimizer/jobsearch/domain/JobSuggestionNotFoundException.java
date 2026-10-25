package com.diegohaefliger.atsresumeoptimizer.jobsearch.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class JobSuggestionNotFoundException extends BusinessException {

	public JobSuggestionNotFoundException() {
		super("Sugestão de vaga não encontrada.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
