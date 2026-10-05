package com.diegohaefliger.atsresumeoptimizer.rewrite.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class AnalysisNotReadyForRewriteException extends BusinessException {

	public AnalysisNotReadyForRewriteException() {
		super("Só dá pra reescrever uma análise que já terminou (COMPLETED ou PARTIAL).");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.CONFLICT;
	}
}
