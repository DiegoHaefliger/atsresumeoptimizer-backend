package com.diegohaefliger.atsresumeoptimizer.linkedin.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class LinkedInAnalysisNotFoundException extends BusinessException {

	public LinkedInAnalysisNotFoundException() {
		super("Análise do LinkedIn não encontrada.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
