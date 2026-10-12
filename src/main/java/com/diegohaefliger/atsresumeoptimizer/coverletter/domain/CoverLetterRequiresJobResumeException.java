package com.diegohaefliger.atsresumeoptimizer.coverletter.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CoverLetterRequiresJobResumeException extends BusinessException {

	public CoverLetterRequiresJobResumeException(UUID resumeVersionId) {
		super("Escolha um currículo adaptado para esta vaga antes de gerar a apresentação: %s".formatted(resumeVersionId));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.UNPROCESSABLE_ENTITY;
	}
}
