package com.diegohaefliger.atsresumeoptimizer.jobsearch.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;

public class IncompleteSearchCriteriaException extends BusinessException {

	public IncompleteSearchCriteriaException(List<String> missing) {
		super("Preencha nas Preferências antes de buscar: " + String.join(", ", missing) + ".");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.UNPROCESSABLE_ENTITY;
	}
}
