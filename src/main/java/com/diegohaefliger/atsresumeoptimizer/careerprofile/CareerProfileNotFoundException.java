package com.diegohaefliger.atsresumeoptimizer.careerprofile;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class CareerProfileNotFoundException extends BusinessException {

	public CareerProfileNotFoundException() {
		super("Perfil de carreira ainda não configurado.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
