package com.diegohaefliger.atsresumeoptimizer.chat.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidChatFileException extends BusinessException {

	private final HttpStatus status;

	public InvalidChatFileException(String message, HttpStatus status) {
		super(message);
		this.status = status;
	}

	@Override
	public HttpStatus status() {
		return status;
	}
}
