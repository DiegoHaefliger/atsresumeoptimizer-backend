package com.diegohaefliger.atsresumeoptimizer.resume.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class EmptyResumeFileException extends BusinessException {

	public EmptyResumeFileException() {
		super("Arquivo obrigatório não enviado ou está vazio.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_REQUEST;
	}
}
