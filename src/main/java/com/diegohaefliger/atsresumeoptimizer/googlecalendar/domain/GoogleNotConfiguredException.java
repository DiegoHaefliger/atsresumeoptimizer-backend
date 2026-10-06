package com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class GoogleNotConfiguredException extends BusinessException {

	public GoogleNotConfiguredException() {
		super("Integração com o Google não configurada no servidor.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_REQUEST;
	}
}
