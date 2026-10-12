package com.diegohaefliger.atsresumeoptimizer.interview.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class InterviewRequiresJobResumeException extends BusinessException {

	public InterviewRequiresJobResumeException(UUID resumeVersionId) {
		super("Escolha um currículo adaptado para esta vaga: %s".formatted(resumeVersionId));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.UNPROCESSABLE_ENTITY;
	}
}
