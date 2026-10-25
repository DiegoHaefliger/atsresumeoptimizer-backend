package com.diegohaefliger.atsresumeoptimizer.jobsearch.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidSuggestionStateException extends BusinessException {

	public InvalidSuggestionStateException() {
		super("Essa sugestão já foi promovida e não pode ser descartada.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.CONFLICT;
	}
}
