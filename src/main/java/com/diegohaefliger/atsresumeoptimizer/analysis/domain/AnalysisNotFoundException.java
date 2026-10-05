package com.diegohaefliger.atsresumeoptimizer.analysis.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class AnalysisNotFoundException extends BusinessException {

	public AnalysisNotFoundException(AnalysisId id) {
		super("Análise não encontrada: %s".formatted(id));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
