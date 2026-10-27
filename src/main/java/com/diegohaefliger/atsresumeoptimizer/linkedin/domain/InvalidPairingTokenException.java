package com.diegohaefliger.atsresumeoptimizer.linkedin.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidPairingTokenException extends BusinessException {

	public InvalidPairingTokenException() {
		super("Token de pareamento ausente ou inválido.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.UNAUTHORIZED;
	}
}
