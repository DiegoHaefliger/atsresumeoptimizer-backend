package com.diegohaefliger.atsresumeoptimizer.analysis.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class KeywordsNotEditableException extends BusinessException {

	public KeywordsNotEditableException() {
		super("Só análises de uma vaga têm palavras-chave editáveis.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_REQUEST;
	}
}
