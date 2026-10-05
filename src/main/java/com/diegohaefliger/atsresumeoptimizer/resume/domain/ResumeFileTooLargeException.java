package com.diegohaefliger.atsresumeoptimizer.resume.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class ResumeFileTooLargeException extends BusinessException {

	public ResumeFileTooLargeException() {
		super("Arquivo maior que 2 MB.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.CONTENT_TOO_LARGE;
	}
}
