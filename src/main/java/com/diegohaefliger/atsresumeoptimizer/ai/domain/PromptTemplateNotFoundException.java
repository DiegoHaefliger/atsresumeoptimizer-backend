package com.diegohaefliger.atsresumeoptimizer.ai.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class PromptTemplateNotFoundException extends BusinessException {

	public PromptTemplateNotFoundException(String message) {
		super(message);
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
