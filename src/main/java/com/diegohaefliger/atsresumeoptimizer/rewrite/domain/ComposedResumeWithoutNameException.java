package com.diegohaefliger.atsresumeoptimizer.rewrite.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class ComposedResumeWithoutNameException extends BusinessException {

	public ComposedResumeWithoutNameException() {
		super("Informe o nome do candidato pra criar o currículo.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_REQUEST;
	}
}
