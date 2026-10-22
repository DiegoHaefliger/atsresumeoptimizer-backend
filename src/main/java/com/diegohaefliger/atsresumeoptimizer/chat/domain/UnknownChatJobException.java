package com.diegohaefliger.atsresumeoptimizer.chat.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class UnknownChatJobException extends BusinessException {

	public UnknownChatJobException(UUID jobPostingId) {
		super("Vaga não encontrada: %s".formatted(jobPostingId));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
