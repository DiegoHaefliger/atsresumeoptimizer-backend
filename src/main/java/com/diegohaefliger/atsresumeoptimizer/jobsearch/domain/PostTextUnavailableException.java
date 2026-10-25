package com.diegohaefliger.atsresumeoptimizer.jobsearch.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class PostTextUnavailableException extends BusinessException {

	public PostTextUnavailableException() {
		super("Não consegui ler a postagem sem login. Cole o texto dela no campo abaixo.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.UNPROCESSABLE_ENTITY;
	}
}
