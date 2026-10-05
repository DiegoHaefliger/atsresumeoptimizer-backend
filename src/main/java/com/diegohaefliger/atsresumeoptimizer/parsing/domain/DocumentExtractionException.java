package com.diegohaefliger.atsresumeoptimizer.parsing.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class DocumentExtractionException extends BusinessException {

	public DocumentExtractionException(SourceFormat sourceFormat, Throwable cause) {
		super("Não foi possível extrair o texto do arquivo (%s).".formatted(sourceFormat));
		initCause(cause);
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.UNPROCESSABLE_ENTITY;
	}
}
