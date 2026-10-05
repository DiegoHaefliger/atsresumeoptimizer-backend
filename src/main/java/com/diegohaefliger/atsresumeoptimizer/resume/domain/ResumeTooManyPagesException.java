package com.diegohaefliger.atsresumeoptimizer.resume.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class ResumeTooManyPagesException extends BusinessException {

	public ResumeTooManyPagesException(int maxPages) {
		super("Currículo com mais de %d páginas.".formatted(maxPages));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.CONTENT_TOO_LARGE;
	}
}
