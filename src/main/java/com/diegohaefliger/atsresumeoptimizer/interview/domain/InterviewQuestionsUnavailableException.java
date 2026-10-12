package com.diegohaefliger.atsresumeoptimizer.interview.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class InterviewQuestionsUnavailableException extends BusinessException {

	public InterviewQuestionsUnavailableException() {
		super("A IA não devolveu perguntas. Tenta gerar de novo.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_GATEWAY;
	}
}
