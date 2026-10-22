package com.diegohaefliger.atsresumeoptimizer.chat.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class UnknownChatFileException extends BusinessException {

	public UnknownChatFileException(UUID fileId) {
		super("Arquivo não encontrado: %s".formatted(fileId));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
