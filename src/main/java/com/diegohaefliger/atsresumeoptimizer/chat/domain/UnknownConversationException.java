package com.diegohaefliger.atsresumeoptimizer.chat.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class UnknownConversationException extends BusinessException {

	public UnknownConversationException(UUID conversationId) {
		super("Conversa não encontrada: %s".formatted(conversationId));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
