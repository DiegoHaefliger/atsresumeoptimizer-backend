package com.diegohaefliger.atsresumeoptimizer.notification.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class NotificationNotFoundException extends BusinessException {

	public NotificationNotFoundException(UUID id) {
		super("Notificação não encontrada: %s".formatted(id));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
