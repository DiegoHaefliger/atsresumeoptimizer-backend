package com.diegohaefliger.atsresumeoptimizer.jobsearch.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class PostAlreadyImportedException extends BusinessException {

	public PostAlreadyImportedException() {
		super("Essa postagem já foi importada.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.CONFLICT;
	}
}
