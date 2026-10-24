package com.diegohaefliger.atsresumeoptimizer.jobsearch.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class SearchDisabledException extends BusinessException {

	public SearchDisabledException() {
		super("Ative a busca e informe ao menos um cargo ou palavra-chave nas Preferências.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.CONFLICT;
	}
}
