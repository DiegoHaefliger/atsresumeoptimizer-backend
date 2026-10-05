package com.diegohaefliger.atsresumeoptimizer.resume.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class FavoriteResumeMustBeBaseException extends BusinessException {

	public FavoriteResumeMustBeBaseException(UUID id) {
		super("só currículo base pode ser favorito: %s".formatted(id));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.UNPROCESSABLE_ENTITY;
	}
}
