package com.diegohaefliger.atsresumeoptimizer.parsing.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class UnsupportedDocumentTypeException extends BusinessException {

	public UnsupportedDocumentTypeException(String detectedMimeType) {
		super("Tipo de arquivo não suportado: %s. Envie PDF ou DOCX.".formatted(detectedMimeType));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.UNSUPPORTED_MEDIA_TYPE;
	}
}
