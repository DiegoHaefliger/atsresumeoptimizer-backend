package com.diegohaefliger.atsresumeoptimizer.rewrite.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class RewriteFailedException extends BusinessException {

	public RewriteFailedException(Throwable cause) {
		this(cause.getMessage());
	}

	public RewriteFailedException(String reason) {
		super("Não deu pra reescrever agora: %s".formatted(reason));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_GATEWAY;
	}
}
