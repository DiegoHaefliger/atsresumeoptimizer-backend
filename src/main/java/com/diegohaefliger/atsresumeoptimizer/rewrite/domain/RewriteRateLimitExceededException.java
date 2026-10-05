package com.diegohaefliger.atsresumeoptimizer.rewrite.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class RewriteRateLimitExceededException extends BusinessException {

	public RewriteRateLimitExceededException() {
		super("Limite de reescritas atingido. Tenta de novo mais tarde.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.TOO_MANY_REQUESTS;
	}
}
