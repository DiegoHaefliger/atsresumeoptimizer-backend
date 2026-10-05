package com.diegohaefliger.atsresumeoptimizer.resume.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ResumeVersionNotFoundException extends BusinessException {

	public ResumeVersionNotFoundException(UUID id) {
		super("resume_version não encontrado: %s".formatted(id));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
