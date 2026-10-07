package com.diegohaefliger.atsresumeoptimizer.coverletter.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import org.springframework.http.HttpStatus;

public class CoverLetterRequiresJobException extends BusinessException {

	public CoverLetterRequiresJobException(AnalysisId id) {
		super("A apresentação é feita pra uma vaga; esta análise não tem vaga: %s".formatted(id));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.UNPROCESSABLE_ENTITY;
	}
}
