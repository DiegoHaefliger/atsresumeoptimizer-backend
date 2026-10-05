package com.diegohaefliger.atsresumeoptimizer.job.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class DuplicateJobPostingException extends BusinessException {

	public DuplicateJobPostingException() {
		super("Já existe outra vaga cadastrada com esse mesmo texto.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.CONFLICT;
	}
}
