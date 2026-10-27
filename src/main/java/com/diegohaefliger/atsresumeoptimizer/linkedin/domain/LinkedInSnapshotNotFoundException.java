package com.diegohaefliger.atsresumeoptimizer.linkedin.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class LinkedInSnapshotNotFoundException extends BusinessException {

	public LinkedInSnapshotNotFoundException() {
		super("Nenhum retrato do LinkedIn encontrado. Envie o perfil pela extensão.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
