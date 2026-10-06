package com.diegohaefliger.atsresumeoptimizer.selection.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class StageMovementNotFoundException extends BusinessException {

	public StageMovementNotFoundException(UUID movementId) {
		super("Etapa do histórico não encontrada: %s".formatted(movementId));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
