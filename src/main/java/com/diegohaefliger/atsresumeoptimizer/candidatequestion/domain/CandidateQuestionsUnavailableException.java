package com.diegohaefliger.atsresumeoptimizer.candidatequestion.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class CandidateQuestionsUnavailableException extends BusinessException {

	public CandidateQuestionsUnavailableException() {
		super("A IA não devolveu perguntas. Tenta gerar de novo.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_GATEWAY;
	}
}
