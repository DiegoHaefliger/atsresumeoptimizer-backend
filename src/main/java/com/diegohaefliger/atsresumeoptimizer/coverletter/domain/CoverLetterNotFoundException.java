package com.diegohaefliger.atsresumeoptimizer.coverletter.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CoverLetterNotFoundException extends BusinessException {

	public CoverLetterNotFoundException(UUID id) {
		super("Apresentação não encontrada: %s".formatted(id));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
