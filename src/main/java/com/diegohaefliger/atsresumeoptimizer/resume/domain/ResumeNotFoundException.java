package com.diegohaefliger.atsresumeoptimizer.resume.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ResumeNotFoundException extends BusinessException {

	public ResumeNotFoundException(UUID id) {
		super("resume não encontrado: %s".formatted(id));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
