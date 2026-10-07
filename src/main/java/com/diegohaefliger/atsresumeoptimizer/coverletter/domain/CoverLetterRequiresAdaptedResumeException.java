package com.diegohaefliger.atsresumeoptimizer.coverletter.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import org.springframework.http.HttpStatus;

public class CoverLetterRequiresAdaptedResumeException extends BusinessException {

	public CoverLetterRequiresAdaptedResumeException(AnalysisId id) {
		super("Adapte o currículo pra vaga antes de gerar a apresentação: %s".formatted(id));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.CONFLICT;
	}
}
