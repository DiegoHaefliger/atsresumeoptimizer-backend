package com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class GoogleNotConfiguredException extends BusinessException {

	public GoogleNotConfiguredException() {
		super("Cadastre o client ID e o client secret do Google antes de conectar.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_REQUEST;
	}
}
