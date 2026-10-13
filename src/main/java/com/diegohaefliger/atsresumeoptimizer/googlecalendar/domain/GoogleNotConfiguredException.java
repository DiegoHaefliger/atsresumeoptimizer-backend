package com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class GoogleNotConfiguredException extends BusinessException {

	public GoogleNotConfiguredException() {
		super("Integração com o Google ainda não configurada.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_REQUEST;
	}
}
