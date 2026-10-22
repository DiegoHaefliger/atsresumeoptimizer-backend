package com.diegohaefliger.atsresumeoptimizer.chat.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class NothingToRegenerateException extends BusinessException {

	public NothingToRegenerateException() {
		super("A conversa não tem pergunta para responder de novo.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.CONFLICT;
	}
}
